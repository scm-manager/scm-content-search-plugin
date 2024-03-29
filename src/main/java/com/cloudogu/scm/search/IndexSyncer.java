/*
 * MIT License
 *
 * Copyright (c) 2020-present Cloudogu GmbH and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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

import javax.inject.Inject;
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
