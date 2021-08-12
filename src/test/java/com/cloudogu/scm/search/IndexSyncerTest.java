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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Feature;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;

import java.io.IOException;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexSyncerTest {

  @Mock
  private RepositoryServiceFactory repositoryServiceFactory;

  @Mock
  private RepositoryService repositoryService;

  @Mock
  private IndexerFactory indexerFactory;

  @Mock
  private IndexSyncWorkerFactory indexSyncWorkerFactory;

  @InjectMocks
  private IndexSyncer indexSyncer;

  private final Repository repository = RepositoryTestData.createHeartOfGold();

  @BeforeEach
  void setUp() {
    when(repositoryServiceFactory.create(repository)).thenReturn(repositoryService);
  }

  @Test
  void shouldNotOpenIndexIfLogCommandIsNotSupported() {
    support(false, true, true);

    indexSyncer.ensureIndexIsUpToDate(repository);

    verifyNoInteractions(indexerFactory);
  }

  @Test
  void shouldNotOpenIndexIfBrowseCommandIsNotSupported() {
    support(true, false, true);

    indexSyncer.ensureIndexIsUpToDate(repository);

    verifyNoInteractions(indexerFactory);
  }

  @Test
  void shouldNotOpenIndexIfFeatureIsNotSupported() {
    support(true, true, false);

    indexSyncer.ensureIndexIsUpToDate(repository);

    verifyNoInteractions(indexerFactory);
  }

  @Test
  void shouldCallWorker() throws IOException {
    Indexer indexer = mock(Indexer.class);
    when(indexerFactory.create(repositoryService)).thenReturn(indexer);

    IndexSyncWorker worker = mock(IndexSyncWorker.class);
    when(indexSyncWorkerFactory.create(repositoryService, indexer)).thenReturn(worker);

    support(true, true, true);

    indexSyncer.ensureIndexIsUpToDate(repository);

    verify(worker).ensureIndexIsUpToDate();
  }

  @Test
  void shouldCloseIndexerOnException() throws IOException {
    Indexer indexer = mock(Indexer.class);
    when(indexerFactory.create(repositoryService)).thenReturn(indexer);

    IndexSyncWorker worker = mock(IndexSyncWorker.class);
    when(indexSyncWorkerFactory.create(repositoryService, indexer)).thenReturn(worker);

    doThrow(new IOException("fail")).when(worker).ensureIndexIsUpToDate();

    support(true, true, true);

    indexSyncer.ensureIndexIsUpToDate(repository);

    verify(indexer).close();
  }

  @Test
  void shouldCloseRepositoryServiceOnException() throws IOException {
    Indexer indexer = mock(Indexer.class);
    when(indexerFactory.create(repositoryService)).thenReturn(indexer);

    IndexSyncWorker worker = mock(IndexSyncWorker.class);
    when(indexSyncWorkerFactory.create(repositoryService, indexer)).thenReturn(worker);

    doThrow(new IOException("fail")).when(worker).ensureIndexIsUpToDate();

    support(true, true, true);

    indexSyncer.ensureIndexIsUpToDate(repository);

    verify(repositoryService).close();
  }

  private void support(boolean log, boolean browse, boolean baseMod) {
    lenient().doReturn(log).when(repositoryService).isSupported(Command.LOG);
    lenient().doReturn(browse).when(repositoryService).isSupported(Command.BROWSE);
    lenient().doReturn(baseMod).when(repositoryService).isSupported(Feature.MODIFICATIONS_BETWEEN_REVISIONS);
  }

}
