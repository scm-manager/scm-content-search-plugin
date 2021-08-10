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
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.io.ContentType;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.search.Id;
import sonia.scm.search.Index;
import sonia.scm.search.SearchEngine;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("UnstableApiUsage")
@ExtendWith(MockitoExtension.class)
class IndexerTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private SearchEngine searchEngine;

  @Mock
  private RepositoryService repositoryService;

  @Mock
  private FileContentFactory fileContentFactory;

  @InjectMocks
  private IndexerFactory indexerFactory;

  private Indexer indexer;

  private final Repository repository = RepositoryTestData.createHeartOfGold();

  @BeforeEach
  void setUp() {
    when(repositoryService.getRepository()).thenReturn(repository);
    indexer = indexerFactory.create(repositoryService);
  }

  @Test
  void shouldNotOpenIndexForEmptyStores() throws IOException {
    indexer.store("4211", Collections.emptyList());

    verifyNoInteractions(searchEngine);
  }

  @Test
  void shouldNotOpenIndexForEmptyDeletes() {
    indexer.delete(Collections.emptyList());

    verifyNoInteractions(searchEngine);
  }

  @Test
  void shouldStore() throws IOException {
    Index<FileContent> index = mockIndex();

    FileContent a = new FileContent("21", "a", contentType());
    when(fileContentFactory.create(repositoryService, "42", "a")).thenReturn(a);

    FileContent b = new FileContent("42", "b", contentType());
    when(fileContentFactory.create(repositoryService, "42", "b")).thenReturn(b);

    indexer.store("42", Arrays.asList("a", "b"));

    verify(index).store(Id.of("a").withRepository(repository), "repository:pull:" + repository.getId(), a);
    verify(index).store(Id.of("b").withRepository(repository), "repository:pull:" + repository.getId(), b);
  }

  @Test
  void shouldDelete() {
    Index<FileContent> index = mockIndex();

    Index.ByTypeDeleter deleter = mock(Index.ByTypeDeleter.class);
    when(index.delete().byType()).thenReturn(deleter);

    indexer.delete(Arrays.asList("a", "b"));

    verify(deleter).byId(Id.of("a").withRepository(repository));
    verify(deleter).byId(Id.of("b").withRepository(repository));
  }

  @Test
  void shouldDeleteAll() {
    Index<FileContent> index = mockIndex();

    Index.ByTypeDeleter deleter = mock(Index.ByTypeDeleter.class);
    when(index.delete().byType()).thenReturn(deleter);

    indexer.deleteAll();

    verify(deleter).byRepository(repository.getId());
  }

  @Test
  void shouldNotCloseIfIndexIsNotOpen() {
    indexer.close();

    verifyNoInteractions(searchEngine);
  }

  @Test
  void shouldCloseIndex() {
    Index<FileContent> index = mockIndex();
    Index.ByTypeDeleter deleter = mock(Index.ByTypeDeleter.class);
    when(index.delete().byType()).thenReturn(deleter);
    indexer.deleteAll();

    indexer.close();

    verify(index).close();
  }

  @SuppressWarnings("unchecked")
  private Index<FileContent> mockIndex() {
    Index<FileContent> index = mock(Index.class, Answers.RETURNS_DEEP_STUBS);
    when(searchEngine.forType(FileContent.class).getOrCreate()).thenReturn(index);
    return index;
  }

  private ContentType contentType() {
    ContentType contentType = mock(ContentType.class);
    when(contentType.getRaw()).thenReturn("application/octet-stream");
    return contentType;
  }

}
