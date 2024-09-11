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

import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.repository.Feature;
import sonia.scm.repository.Repository;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.search.Index;

import jakarta.inject.Inject;
import java.io.IOException;

@SuppressWarnings("UnstableApiUsage")
public class IndexSyncer {

  private static final Logger LOG = LoggerFactory.getLogger(IndexSyncer.class);

  private final RepositoryServiceFactory repositoryServiceFactory;
  private final IndexerFactory indexerFactory;
  private final IndexSyncWorkerFactory indexSyncWorkerFactory;

  @Inject
  public IndexSyncer(RepositoryServiceFactory repositoryServiceFactory, IndexerFactory indexerFactory, IndexSyncWorkerFactory indexSyncWorkerFactory) {
    this.repositoryServiceFactory = repositoryServiceFactory;
    this.indexerFactory = indexerFactory;
    this.indexSyncWorkerFactory = indexSyncWorkerFactory;
  }

  public void ensureIndexIsUpToDate(Index<FileContent> index, Repository repository) {
    try (RepositoryService repositoryService = repositoryServiceFactory.create(repository)) {
      if (isSupported(repositoryService)) {
       ensureIndexIsUpToDate(index, repositoryService);
      } else {
        LOG.warn("repository {} could not index, because it does not support combined modifications", repository);
      }
    } catch (IOException e) {
      LOG.error("failed to update index or to check if an update is required for repository {}", repository, e);
    }
  }

  void reindex(Index<FileContent> index, Repository repository) {
    try (RepositoryService repositoryService = repositoryServiceFactory.create(repository)) {
      if (isSupported(repositoryService)) {
        Stopwatch sw = Stopwatch.createStarted();
        Indexer indexer = indexerFactory.create(index, repositoryService);
        try {
            IndexSyncWorker worker = indexSyncWorkerFactory.create(repositoryService, indexer);
            worker.reIndex();
        } finally {
          LOG.debug("re-index operation finished in {}", sw.stop());
        }
      } else {
        LOG.warn("repository {} could not index, because it does not support combined modifications", repository);
      }
    } catch (IOException e) {
      LOG.error("failed to update index or to check if an update is required for repository {}", repository, e);
    }
  }

  private boolean isSupported(RepositoryService repositoryService) {
    return repositoryService.isSupported(Command.LOG)
      && repositoryService.isSupported(Command.BROWSE)
      && repositoryService.isSupported(Feature.MODIFICATIONS_BETWEEN_REVISIONS);
  }

  private void ensureIndexIsUpToDate(Index<FileContent> index, RepositoryService repositoryService) throws IOException {
    Stopwatch sw = Stopwatch.createStarted();
    Indexer indexer = indexerFactory.create(index, repositoryService);
    try {
      IndexSyncWorker worker = indexSyncWorkerFactory.create(repositoryService, indexer);
      worker.ensureIndexIsUpToDate();
    } finally {
      LOG.debug("ensure index is up to date operation finished in {}", sw.stop());
    }
  }

}
