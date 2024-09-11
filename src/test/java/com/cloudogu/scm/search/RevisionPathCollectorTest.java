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
import sonia.scm.repository.BrowserResult;
import sonia.scm.repository.FileObject;
import sonia.scm.repository.api.BrowseCommandBuilder;
import sonia.scm.repository.api.RepositoryService;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevisionPathCollectorTest {

  @Mock
  private RepositoryService repositoryService;

  @InjectMocks
  private RevisionPathCollector collector;

  @Mock(answer = Answers.RETURNS_SELF)
  private BrowseCommandBuilder browseCommand;

  @BeforeEach
  void setUp() {
    when(repositoryService.getBrowseCommand()).thenReturn(browseCommand);
  }

  @Test
  void shouldCollectPaths() throws IOException {
    BrowserResult result = new BrowserResult("42", createTree());
    when(browseCommand.getBrowserResult()).thenReturn(result);

    collector.collect("42");

    verify(browseCommand).setRevision("42");

    assertThat(collector.getPathToStore()).contains("a/b", "a/c");
    assertThat(collector.getPathToDelete()).isEmpty();
  }

  private FileObject createTree() {
    FileObject root = new FileObject();
    root.setName("");
    root.setPath("");
    root.setDirectory(true);

    FileObject a = new FileObject();
    a.setName("a");
    a.setPath("a");
    a.setDirectory(true);

    FileObject b = new FileObject();
    b.setName("b");
    b.setPath("a/b");
    b.setDirectory(false);

    FileObject c = new FileObject();
    c.setName("c");
    c.setPath("a/c");
    c.setDirectory(false);

    a.setChildren(Arrays.asList(b, c));

    root.setChildren(Collections.singletonList(a));

    return root;
  }
}
