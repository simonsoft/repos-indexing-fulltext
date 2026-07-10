/**
 * Copyright (C) 2004-2012 Repos Mjukvara AB
 */
package se.repos.indexing.fulltext;

import se.simonsoft.cms.item.RepoRevision;

public record PostCommitEvent(RepoRevision revision) {
}
