/*
 * Copyright (c) 2020 - present Cloudogu GmbH
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package com.cloudogu.scm.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.repository.Repository;

import java.io.IOException;
import java.util.Optional;

class IndexSyncWorker {

  private static final Logger LOG = LoggerFactory.getLogger(IndexSyncWorker.class);

  private final LatestRevisionResolver latestRevisionResolver;
  private final UpdatePathCollector updatePathCollector;
  private final RevisionPathCollector revisionPathCollector;
  private final IndexStatusStore indexStatusStore;
  private final Indexer indexer;

  private final Repository repository;

  IndexSyncWorker(IndexingContext indexingContext) {
    this.latestRevisionResolver = indexingContext.getLatestRevisionResolver();
    this.updatePathCollector = indexingContext.getUpdatePathCollector();
    this.revisionPathCollector = indexingContext.getRevisionPathCollector();
    this.indexStatusStore = indexingContext.getIndexStatusStore();
    this.indexer = indexingContext.getIndexer();
    this.repository = indexingContext.getRepository();
  }

  public void ensureIndexIsUpToDate() throws IOException {
    Optional<IndexStatus> status = indexStatusStore.get(repository);
    if (status.isPresent()) {
      IndexStatus indexStatus = status.get();
      if (indexStatus.getVersion() != FileContent.VERSION) {
        LOG.debug(
          "found index of repository {} in version {} required is {}, trigger reindex",
          repository, indexStatus.getVersion(), FileContent.VERSION
        );
        reIndex();
      } else if (indexStatus.isEmpty()) {
        reIndex();
      } else {
        ensureIndexIsUpToDate(indexStatus.getRevision());
      }
    } else {
      LOG.debug("no index status present for repository {} trigger reindex", repository);
      reIndex();
    }
  }

  private void ensureIndexIsUpToDate(String revision) throws IOException {
    Optional<String> latestRevision = latestRevisionResolver.resolve();
    if (latestRevision.isPresent()) {
      ensureIndexIsUpToDate(revision, latestRevision.get());
    } else {
      emptyRepository();
    }
  }

  private void ensureIndexIsUpToDate(String from, String to) throws IOException {
    if (from.equals(to)) {
      LOG.debug("index of repository {} is up to date", repository);
      return;
    }

    LOG.debug("start updating index of repository {} from {} to {}", repository, from, to);

    updatePathCollector.collect(from, to);
    updateIndex(to, updatePathCollector);
  }

  private void updateIndex(String revision, PathCollector collector) throws IOException {
    indexer.delete(collector.getPathToDelete());
    indexer.store(revision, collector.getPathToStore());

    indexStatusStore.update(repository, revision);
  }

  void reIndex() throws IOException {
    LOG.debug("start re indexing for repository {}", repository);
    indexer.deleteAll();

    Optional<String> latestRevision = latestRevisionResolver.resolve();
    if (latestRevision.isPresent()) {
      String revision = latestRevision.get();
      revisionPathCollector.collect(latestRevision.get());
      updateIndex(revision, revisionPathCollector);
    } else {
      indexStatusStore.empty(repository);
    }
  }

  private void emptyRepository() {
    LOG.debug("repository {} looks empty, delete all to clean up", repository);
    indexer.deleteAll();
    indexStatusStore.empty(repository);
  }

}
