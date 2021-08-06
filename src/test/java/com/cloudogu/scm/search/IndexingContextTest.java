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
