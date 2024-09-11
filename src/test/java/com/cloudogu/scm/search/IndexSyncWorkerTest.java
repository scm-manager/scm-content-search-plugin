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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.store.InMemoryByteDataStoreFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexSyncWorkerTest {

  @Mock
  private RepositoryService repositoryService;

  @Mock
  private Indexer indexer;

  @Mock
  private IndexingContextFactory contextFactory;

  @Mock
  private IndexingContext context;

  @InjectMocks
  private IndexSyncWorkerFactory factory;

  private IndexStatusStore statusStore;

  @Mock
  private LatestRevisionResolver latestRevisionResolver;

  @Mock
  private UpdatePathCollector updatePathCollector;

  @Mock
  private RevisionPathCollector revisionPathCollector;

  private IndexSyncWorker worker;

  private final Repository repository = RepositoryTestData.createHappyVerticalPeopleTransporter();

  @BeforeEach
  void setUp() {
    when(contextFactory.create(repositoryService, indexer)).thenReturn(context);

    statusStore = new IndexStatusStore(new InMemoryByteDataStoreFactory());

    when(context.getIndexStatusStore()).thenReturn(statusStore);
    when(context.getLatestRevisionResolver()).thenReturn(latestRevisionResolver);
    when(context.getUpdatePathCollector()).thenReturn(updatePathCollector);
    when(context.getRevisionPathCollector()).thenReturn(revisionPathCollector);
    when(context.getIndexer()).thenReturn(indexer);
    when(context.getRepository()).thenReturn(repository);

    worker = factory.create(repositoryService, indexer);
  }

  @Test
  void shouldDeleteAllForNonIndexedEmptyRepository() throws IOException {
    worker.ensureIndexIsUpToDate();

    verify(indexer).deleteAll();
    assertThat(statusStore.get(repository))
      .hasValueSatisfying(status -> assertThat(status.isEmpty()).isTrue());
  }

  @Test
  void shouldReIndexNonIndexedRepository() throws IOException {
    when(latestRevisionResolver.resolve()).thenReturn(Optional.of("42"));
    List<String> pathToStore = Arrays.asList("a", "b");
    when(revisionPathCollector.getPathToStore()).thenReturn(pathToStore);

    worker.ensureIndexIsUpToDate();

    verify(indexer).deleteAll();
    verify(revisionPathCollector).collect("42");
    verify(indexer).store("42", pathToStore);
    assertThat(statusStore.get(repository))
      .hasValueSatisfying(status -> assertThat(status.getRevision()).isEqualTo("42"));
  }

  @Test
  void shouldDeleteAllForEmptyRepository() throws IOException {
    statusStore.update(repository, "42");

    worker.ensureIndexIsUpToDate();

    verify(indexer).deleteAll();
    assertThat(statusStore.get(repository))
      .hasValueSatisfying(status -> assertThat(status.isEmpty()).isTrue());
  }

  @Test
  void shouldReIndexIfIndexVersionHasChanged() throws IOException {
    statusStore.update(repository, "42", -1);
    when(latestRevisionResolver.resolve()).thenReturn(Optional.of("42"));

    worker.ensureIndexIsUpToDate();

    verify(indexer).deleteAll();
    assertThat(statusStore.get(repository))
      .hasValueSatisfying(status -> {
        assertThat(status.getRevision()).isEqualTo("42");
        assertThat(status.getLastUpdate()).isNotNull();
        assertThat(status.getVersion()).isEqualTo(FileContent.VERSION);
      });
  }

  @Test
  void shouldReIndexIfStatusIsEmpty() throws IOException {
    statusStore.empty(repository);

    List<String> pathToStore = Arrays.asList("a", "b");
    when(revisionPathCollector.getPathToStore()).thenReturn(pathToStore);
    when(latestRevisionResolver.resolve()).thenReturn(Optional.of("42"));

    worker.ensureIndexIsUpToDate();

    verify(indexer).deleteAll();
    verify(revisionPathCollector).collect("42");
    verify(indexer).store("42", pathToStore);
    assertThat(statusStore.get(repository))
      .hasValueSatisfying(status -> assertThat(status.getRevision()).isEqualTo("42"));
  }

  @Test
  void shouldDoNothingIfIndexIsUpToDate() throws IOException {
    statusStore.update(repository, "42");
    when(latestRevisionResolver.resolve()).thenReturn(Optional.of("42"));

    worker.ensureIndexIsUpToDate();

    verifyNoInteractions(indexer);
  }

  @Test
  void shouldUpdateIndex() throws IOException {
    statusStore.update(repository, "21");
    when(latestRevisionResolver.resolve()).thenReturn(Optional.of("42"));

    List<String> pathToStore = Arrays.asList("a", "b");
    when(updatePathCollector.getPathToStore()).thenReturn(pathToStore);
    List<String> pathToDelete = Arrays.asList("c", "d");
    when(updatePathCollector.getPathToDelete()).thenReturn(pathToDelete);

    worker.ensureIndexIsUpToDate();

    verify(updatePathCollector).collect("21", "42");
    verify(indexer).delete(pathToDelete);
    verify(indexer).store("42", pathToStore);
    verify(indexer, never()).deleteAll();
    assertThat(statusStore.get(repository))
      .hasValueSatisfying(status -> assertThat(status.getRevision()).isEqualTo("42"));
  }

  @Test
  void shouldReindex() throws IOException {
    when(latestRevisionResolver.resolve()).thenReturn(Optional.of("42"));
    List<String> pathToStore = Arrays.asList("a", "b");
    when(revisionPathCollector.getPathToStore()).thenReturn(pathToStore);

    worker.reIndex();

    verify(indexer).deleteAll();
    verify(revisionPathCollector).collect("42");
    verify(indexer).store("42", pathToStore);
    assertThat(statusStore.get(repository))
      .hasValueSatisfying(status -> assertThat(status.getRevision()).isEqualTo("42"));
  }
}
