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
