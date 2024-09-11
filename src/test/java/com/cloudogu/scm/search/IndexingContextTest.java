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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexingContextTest {

  @Mock
  private RepositoryService repositoryService;

  @Mock
  private IndexStatusStore indexStatusStore;

  @Mock
  private Indexer indexer;

  @InjectMocks
  private IndexingContextFactory factory;

  private IndexingContext context;

  @BeforeEach
  void setUp() {
    context = factory.create(repositoryService, indexer);
  }

  @Test
  void shouldReturnRepositoryFromService() {
    Repository repository = RepositoryTestData.create42Puzzle();
    when(repositoryService.getRepository()).thenReturn(repository);

    assertThat(context.getRepository()).isSameAs(repository);
  }

  @Test
  void shouldReturnDependencies() {
    assertThat(context.getIndexStatusStore()).isSameAs(indexStatusStore);
    assertThat(context.getIndexer()).isSameAs(indexer);
  }

  @Test
  void shouldCreateRequiredHelpers() {
    assertThat(context.getLatestRevisionResolver()).isNotNull();
    assertThat(context.getUpdatePathCollector()).isNotNull();
    assertThat(context.getRevisionPathCollector()).isNotNull();
  }
}
