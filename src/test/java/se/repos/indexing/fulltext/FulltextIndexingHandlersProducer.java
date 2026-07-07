/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.fulltext;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

import org.apache.solr.client.solrj.SolrClient;

import se.repos.indexing.IndexingItemHandler;
import se.repos.indexing.config.IndexingHandlersProducer;
import se.repos.indexing.item.ItemContentBufferStrategy;
import se.repos.indexing.item.ItemPropertiesBufferStrategy;
import se.repos.indexing.repository.HandlerContentDisable;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.indexing.IdStrategy;

@Alternative
@Priority(1)
public class FulltextIndexingHandlersProducer {

	@Produces
	@RequestScoped
	public Set<IndexingItemHandler> createIndexingItemHandlers(
			IdStrategy idStrategy,
			@Named("repositem") SolrClient repositem,
			CmsRepository repository,
			ItemContentBufferStrategy contentBufferStrategy,
			ItemPropertiesBufferStrategy propertiesBufferStrategy) {

		Set<IndexingItemHandler> standard = new IndexingHandlersProducer().createIndexingItemHandlers(
				idStrategy, repositem, repository, contentBufferStrategy, propertiesBufferStrategy);
		Set<IndexingItemHandler> handlers = new LinkedHashSet<>();
		boolean fulltextAdded = false;
		for (IndexingItemHandler handler : standard) {
			if (!fulltextAdded && handler.getClass().equals(HandlerContentDisable.class)) {
				handlers.add(new HandlerFulltext());
				fulltextAdded = true;
			}
			handlers.add(handler);
		}
		if (!fulltextAdded) {
			throw new IllegalStateException("Could not place " + HandlerFulltext.class.getSimpleName() + " in handler chain");
		}
		return handlers;
	}
}
