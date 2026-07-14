/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.fulltext;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import se.repos.indexing.ReposIndexing;
import se.repos.indexing.scheduling.IndexingSchedule;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.svn.runtime.SvnRevisionAvailableEvent;

@ApplicationScoped
public class PostCommitIndexingObserver {

	@Inject
	ReposIndexing indexing;

	@Inject
	IndexingSchedule schedule;

	public void onPostCommit(@Observes SvnRevisionAvailableEvent event) {
		schedule.start();
		try {
			indexing.sync(new RepoRevision(event.revision(), null));
		} finally {
			schedule.stop();
		}
	}
}
