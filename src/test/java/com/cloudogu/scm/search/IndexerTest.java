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

  @Mock
  private RepositoryService repositoryService;

  @Mock
  private FileContentFactory fileContentFactory;

  @InjectMocks
  private IndexerFactory indexerFactory;

  private Indexer indexer;

  private final Repository repository = RepositoryTestData.createHeartOfGold();

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private Index<FileContent> index;

  @BeforeEach
  void setUp() {
    when(repositoryService.getRepository()).thenReturn(repository);
    indexer = indexerFactory.create(index, repositoryService);
  }

  @Test
  void shouldNotIndexForEmptyStores() throws IOException {
    indexer.store("4211", Collections.emptyList());

    verifyNoInteractions(index);
  }

  @Test
  void shouldNotOpenIndexForEmptyDeletes() {
    indexer.delete(Collections.emptyList());

    verifyNoInteractions(index);
  }

  @Test
  void shouldStore() throws IOException {
    FileContent a = new FileContent("21", "a", contentType());
    when(fileContentFactory.create(repositoryService, "42", "a")).thenReturn(a);

    FileContent b = new FileContent("42", "b", contentType());
    when(fileContentFactory.create(repositoryService, "42", "b")).thenReturn(b);

    indexer.store("42", Arrays.asList("a", "b"));

    verify(index).store(id("a"), "repository:pull:" + repository.getId(), a);
    verify(index).store(id("b"), "repository:pull:" + repository.getId(), b);
  }

  private Id<FileContent> id(String a) {
    return Id.of(FileContent.class, a).and(Repository.class, repository);
  }

  @Test
  void shouldDelete() {
    Index.Deleter<FileContent> deleter = mock(Index.Deleter.class);
    when(index.delete()).thenReturn(deleter);

    indexer.delete(Arrays.asList("a", "b"));

    verify(deleter).byId(id("a"));
    verify(deleter).byId(id("b"));
  }

  @Test
  void shouldDeleteAll() {
    indexer.deleteAll();

    verify(index.delete().by(Repository.class, repository)).execute();
  }

  private ContentType contentType() {
    ContentType contentType = mock(ContentType.class);
    when(contentType.getRaw()).thenReturn("application/octet-stream");
    return contentType;
  }

}
