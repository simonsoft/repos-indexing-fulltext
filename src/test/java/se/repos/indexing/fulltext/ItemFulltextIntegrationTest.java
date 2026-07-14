/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.fulltext;

import static org.junit.Assert.*;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.SVNRepository;

import jakarta.enterprise.context.control.ActivateRequestContext;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import se.repos.indexing.item.IndexingItemStandalone;
import se.repos.indexing.solrj.SolrAdd;

/**
 * Test queries on files in an actual test repository.
 * 
 * This should just test that the combination of repository and indexing works,
 * for details on extraction and querying use instead
 * {@link ItemFulltextExtractionTest} and {@link ItemFulltextQueryTest}.
 */
@QuarkusTest
@TestProfile(SvnSolrRepositemProfile.class)
public class ItemFulltextIntegrationTest {

	@Inject
	@Named("repositem")
	SolrClient repositem;

	@Inject
	SVNRepository svnkit;
	
	@AfterEach
	public void tearDown() throws SolrServerException, IOException {
		repositem.deleteByQuery("*:*");
		repositem.commit();
	}
	
	/**
	 * Test the index of the structure in Repos Search 1 test sets.
	 * @throws IOException 
	 */
	@Test
	@ActivateRequestContext
	public void testHandleSearch1Docs() throws SVNException, SolrServerException, IOException {
		SolrClient solr = repositem;
		assertEquals(1, svnkit.getLatestRevision());
		
		QueryResponse all = solr.query(new SolrQuery("*:*"));
		assertEquals("Should have indexed all v1 documents (31), folders (9), history (31+9) and commits (2)", 31 + 9 + (31+9) + 2, all.getResults().getNumFound());
		
		QueryResponse pdf = solr.query(new SolrQuery("pathext:pdf AND head:true"));
		assertEquals(1, pdf.getResults().getNumFound());
		SolrDocument shortpdf = pdf.getResults().get(0);
		assertEquals("keywordinsaveaspdf someotherkeyword", shortpdf.getFieldValues("embd_meta.keyword").iterator().next());
		
		/* Causes NPE since introducing XMP metadata. Not sure why.
		assertEquals("keywordinsaveaspdf someotherkeyword", shortpdf.getFieldValues("embd_dc:subject").iterator().next());
		*/
		// don't forget to avoid assertions of stuff that belong in the isolated tests, almost everything
		
		// TODO see https://wiki.apache.org/tika/MetadataRoadmap
		// We need typed values, particularily Date support. We might also want to avoid multi-value for most fields.
	}
	
	@Test
	public void testInvalidXml() throws SolrServerException, IOException {
		IndexingItemStandalone item = new IndexingItemStandalone("se/repos/indexing/fulltext/datasets/tiny-invalidxml/test1.xml");
		item.getFields().setField("id", "invalidxml");
		item.getFields().setField("head", true);

		HandlerFulltext handler = new HandlerFulltext();
		handler.handle(item);
		new SolrAdd(repositem, item.getFields()).run();
		repositem.commit();
		SolrClient solr = repositem;
		
		SolrDocumentList all = solr.query(new SolrQuery("text_error:\"must be terminated\" AND head:true")).getResults();
		assertEquals("should index extraction errors", 1, all.getNumFound());
	}

}
