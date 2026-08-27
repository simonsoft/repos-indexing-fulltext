/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.fulltext;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class SolrRepositemProfile implements QuarkusTestProfile {

	@Override
	public Map<String, String> getConfigOverrides() {
		return Map.of(
				"quarkus.solr.devservices.core", "repositem",
				// TODO Remove these placeholders when the SVN extension allows quarkus.svn.enabled=false without connection config.
				"quarkus.svn.enabled", "false",
				"quarkus.svn.devservices.enabled", "false",
				"quarkus.svn.hostname", "localhost",
				"quarkus.svn.port", "80",
				"quarkus.svn.username", "unused",
				"quarkus.svn.password", "unused");
	}
}
