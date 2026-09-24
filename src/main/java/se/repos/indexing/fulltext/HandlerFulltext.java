/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.fulltext;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.OfficeOpenXMLCore;
import org.apache.tika.xmp.XMPMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.item.HandlerPathinfo;
import se.repos.indexing.item.IndexingItemProgress;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;

import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPIterator;
import com.adobe.xmp.options.PropertyOptions;
import com.adobe.xmp.properties.XMPPropertyInfo;

public class HandlerFulltext implements IndexingItemHandler {

	/**
	 * To avoid multivalue fields for all metadata in Solr we concatenate with newline, and tokenize on that at indexing.
	 */
	private static final String METADATA_MULTIVALUE_SEPARATOR = "\n";
	
	private static final Logger logger = LoggerFactory.getLogger(HandlerFulltext.class);

	static {
		try {
			// Register cp before extraction so PDF metadata does not depend on prior Office files.
			XMPMetadata.registerNamespace(OfficeOpenXMLCore.NAMESPACE_URI, OfficeOpenXMLCore.PREFIX);
		} catch (XMPException e) {
			logger.error("XMP namespace registration failed: {}", e.getMessage(), e);
		}
	}
	
	/**
	 * Because we depend on field updates (the "head" flag) for historical items in incremental indexing,
	 * and because text is stored="false" (see http://wiki.apache.org/solr/Atomic_Updates#Stored_Values),
	 * we should for consistency skip text at reindex where no field update is done.
	 */
	private boolean skipTextForHistorical = true;	
	
	@Override
	public void handle(IndexingItemProgress progress) {
		
		CmsChangesetItem item = progress.getItem();
		
		if (item.isFolder()) {
			logger.trace("Skipping folder {}", item);
			return;
		}
		
		if (item.isDelete()) {
			logger.trace("Skipping deleted {}", item);
			return;
		}

		if (item.getFilesize() == 0) {
			logger.trace("Skipping empty file {}", item);
			return;
		}

		// Indexing both traditional Metadata and XMPMetadata (tika-xmp).
		// TODO pre-load metadata with for example explicit content type from svn prop
		Metadata metadata = new Metadata();
		
		useTika(progress.getContents(), metadata, progress.getFields());
			
	}

	@SuppressWarnings("serial")
	@Override
	public Set<Class<? extends IndexingItemHandler>> getDependencies() {
		return new HashSet<Class<? extends IndexingItemHandler>>() {{
			this.add(HandlerPathinfo.class);
		}};
	}
	
	public void useTika(InputStream content, Metadata metadata, IndexingDoc indexingDoc) {
		Tika tika = new Tika();
		
		String text;
		
		try {
			logger.debug("Starting fulltext extraction");
			text = tika.parseToString(content, metadata, 500000);
		} catch (TikaException e) {
			logger.warn("Content extraction error for {}: {}", indexingDoc.getFieldValue("id"), e.getMessage());
			StringWriter err = new StringWriter();
			e.printStackTrace(new PrintWriter(err));
			indexingDoc.addField("text_error", err.toString());
			return;
		} catch (LinkageError e) {
			// Likely incompatibility btw Tika and IBM JVM when parsing MS Office files.
			logger.error("Content extraction error (JVM incompatible) for {}: {}", indexingDoc.getFieldValue("id"), e.getMessage());
			StringWriter err = new StringWriter();
			e.printStackTrace(new PrintWriter(err));
			indexingDoc.addField("text_error", err.toString());
			return;
		} catch (OutOfMemoryError e) {
			// Allow failure to do fulltext extraction when facing large or broken files, noted as text_error in index.
			logger.error("Content extraction error (Out of Memory) for {}: {}", indexingDoc.getFieldValue("id"), e.getMessage());
			StringWriter err = new StringWriter();
			e.printStackTrace(new PrintWriter(err));
			indexingDoc.addField("text_error", err.toString());
			return;
		} catch (Exception e) {
			// Have to take a more forgiving approach since we are seeing fatal issues when running in IBM JVM.
			logger.error("Content extraction error for {}: {}", indexingDoc.getFieldValue("id"), e.getMessage());
			StringWriter err = new StringWriter();
			e.printStackTrace(new PrintWriter(err));
			indexingDoc.addField("text_error", err.toString());
			return;
		}
		
		if (skipTextForHistorical && indexingDoc.containsKey("head") && indexingDoc.getFieldValue("head").equals(false)) {
			logger.debug("Skipping text field for item {} known to be historical", indexingDoc.getFieldValue("id"));
		} else {
			indexingDoc.setField("text", text);
		}

		// Extract the classic embedded metadata.
		for (String n : metadata.names()) {
			this.addField("embd_", n, metadata, indexingDoc);
		}

		// Extract the metadata after mapping to XMP.
		try {
			XMPMetadata xmpMetadata = new org.apache.tika.xmp.XMPMetadata(metadata);
			Set<String> xmpFields = new HashSet<String>();
			
			// First determine a set of XMP keys defined in this file.
			XMPIterator it = xmpMetadata.getXMPData().iterator();
			while (it.hasNext()) { 
				XMPPropertyInfo xmp = (XMPPropertyInfo) it.next();
				// https://code.google.com/p/metadata-extractor/source/browse/Libraries/XMPCore/src/com/adobe/xmp/options/PropertyOptions.java
				PropertyOptions xmpOptions = xmp.getOptions();
				String xmpPath = xmp.getPath();
				
				String msg = MessageFormatter.format("XMP node: {} ({}): " + xmp.getValue(), xmpPath, xmpOptions.getOptionsString()).getMessage();
				logger.trace(msg);
				//System.out.println(msg);
				
				
				if (xmpPath != null && !xmpPath.isEmpty()) {
					//System.out.println(msg);
					// Unfortunately, it.skipSubtree() does not work as documented.
					boolean isDeep = (xmpPath.contains("/") | xmpPath.contains("["));
					if (isDeep) {
						logger.debug("Skipping XMP node {} on {}", xmpPath, indexingDoc.getFieldValue("id"));
					} else {
						xmpFields.add(xmpPath);
					}
				} 
				// Going deep can give keys like "dc:title[1]/xml:lang" which fails getValues(..)
				// This call does not work properly, need to examine xmpPath for '/' or '[' above.
				/*
				if ((xmpOptions.getOptions() & PropertyOptions.ARRAY) != 0) {
					it.skipSubtree();
					//System.out.println("Skipping subtree");
				}
				*/
			}
				
			for (String n : xmpFields) {
				try {
					String[] values = xmpMetadata.getValues(n);
					if (values != null && values[0] != null) {
						logger.trace("XMP in {} -  field {} exists ({}).", indexingDoc.getFieldValue("id"), n, values.length);
						this.addField("xmp_", n, metadata, indexingDoc);
					} else {
						logger.warn("XMP in {} -  field {} does NOT exist.", indexingDoc.getFieldValue("id"), n);
						//System.out.println("XMP field " + n + " does NOT exist.");
					}
				} catch (Exception e) {
					logger.warn("XMP extraction error field {} on {}: {}", n, indexingDoc.getFieldValue("id"), e.getMessage());
				}

			}
			
		} catch (Exception e) {
			logger.warn("XMP extraction error for {}: {}", indexingDoc.getFieldValue("id"), e.getMessage());
			StringWriter err = new StringWriter();
			e.printStackTrace(new PrintWriter(err));
			indexingDoc.addField("text_error", err.toString());
			return;
		}
	}

	private void addField(String prefix, String n, Metadata metadata, IndexingDoc indexingDoc) {
		
		String fieldname = prefix + n.replace(' ', '_').replace(':', '.');
		String value;
		if (n.indexOf(' ') >= 0) {
			logger.trace("Whitespace in metadata name, '{}' becomes {}", n, fieldname);
		}
		if (n.indexOf(':') >= 0) {
			logger.trace("Colon in metadata name, '{}' becomes {}", n, fieldname);
		}
		if (metadata.isMultiValued(n)) {
			StringBuffer concat = new StringBuffer();
			for (String v : metadata.getValues(n)) {
				concat.append(METADATA_MULTIVALUE_SEPARATOR).append(v);
			}
			value = concat.substring(1);
		} else {
			value = metadata.get(n);
		}
		// #1840 SolR 8+ limits string fields to 32766 bytes (immense term)
		if (value.getBytes(Charset.forName("UTF-8")).length <= 32000) {
			indexingDoc.addField(fieldname, value);
		} else {
			logger.info("Tika metadata field '{}' suppressed, exceeds 32000 bytes: {}", n, indexingDoc.getFieldValue("id"));
		}
	}
}
