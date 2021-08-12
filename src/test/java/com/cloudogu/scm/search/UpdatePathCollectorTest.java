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
import sonia.scm.repository.Added;
import sonia.scm.repository.Copied;
import sonia.scm.repository.Modifications;
import sonia.scm.repository.Modified;
import sonia.scm.repository.Removed;
import sonia.scm.repository.Renamed;
import sonia.scm.repository.api.ModificationsCommandBuilder;
import sonia.scm.repository.api.RepositoryService;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdatePathCollectorTest {

  @Mock
  private RepositoryService repositoryService;

  @InjectMocks
  private UpdatePathCollector collector;

  @Mock(answer = Answers.RETURNS_SELF)
  private ModificationsCommandBuilder modificationsCommand;

  @BeforeEach
  void setUp() {
    when(repositoryService.getModificationsCommand()).thenReturn(modificationsCommand);
  }

  @Test
  void shouldCollectPaths() throws IOException {
    when(modificationsCommand.getModifications()).thenReturn(createModifications());

    collector.collect("21", "42");

    verify(modificationsCommand).baseRevision("21");
    verify(modificationsCommand).revision("42");

    assertThat(collector.getPathToStore()).containsOnly("a", "m", "c", "y");
    assertThat(collector.getPathToDelete()).containsOnly("r", "x");
  }

  private Modifications createModifications() {
    return new Modifications(
      "42",
      new Added("a"),
      new Modified("m"),
      new Copied("a", "c"),
      new Removed("r"),
      new Renamed("x", "y")
    );
  }
}
