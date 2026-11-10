/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.fulltext;

import static org.junit.Assert.*;

import org.junit.Test;

import se.repos.indexing.IndexingDoc;
import se.repos.indexing.item.IndexingItemProgress;
import se.repos.indexing.item.IndexingItemStandalone;

/**
 * Tests what we can get out of various file formats by asserting on an index document, no actual use of Solr.
 */
public class ItemFulltextExtractionTest {

	private void printFields(IndexingDoc fields) {

		for (@SuppressWarnings("unused") String f : fields.getFieldNames()) {
			//System.out.println(f);
		}
	}

	@Test
	public void testEmpty1() {
		HandlerFulltext fulltext = new HandlerFulltext();
		
		IndexingItemProgress item = new IndexingItemStandalone("repos-search-v1/docs/empty.txt");
		fulltext.handle(item);
		IndexingDoc fields = item.getFields();
		
		assertFalse("Should not flag as error, Tika 1.23 throws specific exception", fields.containsKey("text_error"));
		// Tika 1.23: org.apache.tika.exception.ZeroByteFileException
		assertFalse("Text extraction suppressed when empty", fields.containsKey("text"));
	}
	
	
	@Test
	public void testExcel1() {
		HandlerFulltext fulltext = new HandlerFulltext();

		IndexingItemProgress item = new IndexingItemStandalone("repos-search-v1/docs/Excel format.xls");
		fulltext.handle(item);
		IndexingDoc fields = item.getFields();

		assertTrue("Should extract text", fields.containsKey("text"));

		printFields(fields);
		/*
		 * With generic Metadata:
		text
		embd_Revision-Number
		embd_cp:revision
		embd_Last-Printed
		embd_meta:print-date
		embd_meta:creation-date
		embd_dcterms:modified
		embd_meta:save-date
		embd_dcterms:created
		embd_Last-Modified
		embd_date
		embd_modified
		embd_Edit-Time
		embd_Creation-Date
		embd_Content-Type
		embd_Last-Save-Date
		 * 
		 */
		assertEquals("CP Revision", "1", fields.getFieldValue("xmp_cp.revision"));
	}

	@Test
	public void testWord1() {
		HandlerFulltext fulltext = new HandlerFulltext();

		IndexingItemProgress item = new IndexingItemStandalone("repos-search-v1/docs/Word document.doc");
		fulltext.handle(item);
		IndexingDoc fields = item.getFields();

		assertTrue("Should extract text", fields.containsKey("text"));

		printFields(fields);
		/*
		 * With generic Metadata:
		text
		embd_Revision-Number
		embd_cp:revision
		embd_Last-Printed
		embd_meta:print-date
		embd_meta:creation-date
		embd_dcterms:modified
		embd_meta:save-date
		embd_dcterms:created
		embd_Last-Modified
		embd_date
		embd_modified
		embd_Edit-Time
		embd_Creation-Date
		embd_Content-Type
		embd_Last-Save-Date
		 * 
		 */
		//assertEquals("DC Format", "something", fields.getFieldValue("xmp_dc.format"));
		assertEquals("CP Revision", "1", fields.getFieldValue("xmp_cp.revision"));
	}

	@Test
	public void testPdf1() {
		HandlerFulltext fulltext = new HandlerFulltext();

		IndexingItemProgress item = new IndexingItemStandalone("repos-search-v1/docs/svnprops/shortpdf.pdf");
		fulltext.handle(item);
		IndexingDoc fields = item.getFields();

		assertTrue("Should extract text", fields.containsKey("text"));

		printFields(fields);

		// Seen some note about DC-subject should be both keywords and subject. 
		assertEquals("Subject", "The PDF subject", fields.getFieldValue("embd_subject"));
		assertEquals("CP subject without prior Office extraction", "The PDF subject", fields.getFieldValue("xmp_cp.subject"));
		assertEquals("Keywords", "keywordinsaveaspdf someotherkeyword", fields.getFieldValue("embd_meta.keyword"));
		// Field "xmp_dc.subject" changed to null from Tika 1.14 to 1.23.
		//assertEquals("Keywords", "keywordinsaveaspdf someotherkeyword", fields.getFieldValue("xmp_dc.subject"));
		assertEquals("Title", "PDF Title For Short Document", fields.getFieldValue("xmp_dc.title"));
		/*
		 * With generic Metadata:
		text
		embd_dc:subject
		embd_meta:save-date
		embd_subject
		embd_Author
		embd_dcterms:created
		embd_date
		embd_creator
		embd_Creation-Date
		embd_title
		embd_meta:author
		embd_created
		embd_meta:keyword
		embd_cp:subject
		embd_xmp:CreatorTool
		embd_Keywords
		embd_dc:title
		embd_Last-Save-Date
		embd_meta:creation-date
		embd_dcterms:modified
		embd_dc:creator
		embd_Last-Modified
		embd_AAPL:Keywords
		embd_modified
		embd_xmpTPg:NPages
		embd_producer
		embd_Content-Type
		 */
	}

	@Test
	public void testJpgCommentXnview1() {
		HandlerFulltext fulltext = new HandlerFulltext();

		IndexingItemProgress item = new IndexingItemStandalone("repos-search-v1/docs/images/testJPEG_commented_xnviewmp026.jpg");
		fulltext.handle(item);
		IndexingDoc fields = item.getFields();

		// Should we add the text field even if it is empty?
		assertTrue("Should extract text", fields.containsKey("text"));
		assertEquals("Text", "", fields.getFieldValue("text"));
		
		printFields(fields);


		// For this image, the XMP extraction finds namespaces: dc (2), tiff (10), exif (just 7) 
		assertEquals("Subject (keywords)", "bird watching\ncoast\nnature reserve\ngrazelands", fields.getFieldValue("embd_subject"));
		assertEquals("Subject (keywords)", "bird watching\ncoast\nnature reserve\ngrazelands", fields.getFieldValue("xmp_dc.subject"));
		assertEquals("Description", "Bird site in north eastern Skåne, Sweden.\n(new line)", fields.getFieldValue("xmp_dc.description"));
		assertEquals("1", fields.getFieldValue("xmp_tiff.Orientation"));
		
		/*
		 * From generic Metadata:
		text
		embd_GPS_Altitude_Ref
		embd_Model
		embd_Exif_Version
		embd_Exposure_Mode
		embd_dc.description
		embd_tiff.ImageLength
		embd_Creation-Date
		embd_exif.Flash
		embd_ISO_Speed_Ratings
		embd_X_Resolution
		embd_Shutter_Speed_Value
		embd_description
		embd_tiff.ImageWidth
		embd_Jpeg_Comment
		embd_tiff.XResolution
		embd_Image_Width
		embd_Keywords
		embd_GPS_Longitude
		embd_GPS_Longitude_Ref
		embd_exif.FNumber
		embd_GPS_Altitude
		embd_F-Number
		embd_Comments
		embd_Color_Space
		embd_meta.creation-date
		embd_Resolution_Units
		embd_Data_Precision
		embd_tiff.BitsPerSample
		embd_tiff.YResolution
		embd_YCbCr_Positioning
		embd_Compression_Type
		embd_Components_Configuration
		embd_exif.IsoSpeedRatings
		embd_Thumbnail_Offset
		embd_Exif_Image_Height
		embd_Focal_Length
		embd_Thumbnail_Length
		embd_Thumbnail_Compression
		embd_comment
		embd_Content-Type
		embd_White_Balance_Mode
		embd_tiff.Orientation
		embd_Make
		embd_tiff.Make
		embd_Date/Time_Original
		embd_dc.subject
		embd_subject
		embd_Exif_Image_Width
		embd_Scene_Capture_Type
		embd_dcterms.created
		embd_exif.ExposureTime
		embd_Component_1
		embd_GPS_Latitude
		embd_Component_2
		embd_Component_3
		embd_GPS_Latitude_Ref
		embd_tiff.ResolutionUnit
		embd_Flash
		embd_Date/Time_Digitized
		embd_meta.keyword
		embd_Resolution_Unit
		embd_White_Balance
		embd_Number_of_Components
		embd_Aperture_Value
		embd_tiff.Model
		embd_Orientation
		embd_Image_Height
		embd_w.comments
		embd_geo.lat
		embd_Exposure_Time
		embd_exif.DateTimeOriginal
		embd_exif.FocalLength
		embd_FlashPix_Version
		embd_Custom_Rendered
		embd_geo.long
		embd_Digital_Zoom_Ratio
		embd_GPS_Version_ID
		embd_Gain_Control
		embd_Y_Resolution
		 */
	}

}
