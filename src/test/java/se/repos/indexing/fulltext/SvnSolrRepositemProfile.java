/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.fulltext;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class SvnSolrRepositemProfile implements QuarkusTestProfile {

	@Override
	public Map<String, String> getConfigOverrides() {
		return Map.of(
				"quarkus.solr.devservices.core", "repositem",
				"quarkus.svn.devservices.init-dataset-path", "repos-search-v1");
	}
}
