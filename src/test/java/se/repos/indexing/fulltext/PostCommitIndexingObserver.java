/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.fulltext;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import se.repos.indexing.ReposIndexing;
import se.repos.indexing.scheduling.IndexingSchedule;

@ApplicationScoped
public class PostCommitIndexingObserver {

	@Inject
	ReposIndexing indexing;

	@Inject
	IndexingSchedule schedule;

	public void onPostCommit(@Observes PostCommitEvent event) {
		schedule.start();
		try {
			indexing.sync(event.revision());
		} finally {
			schedule.stop();
		}
	}
}
