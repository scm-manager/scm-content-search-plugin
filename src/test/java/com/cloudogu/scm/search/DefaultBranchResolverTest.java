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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Branch;
import sonia.scm.repository.Branches;
import sonia.scm.repository.Feature;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.RepositoryService;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultBranchResolverTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private RepositoryService repositoryService;

  @InjectMocks
  private DefaultBranchResolver resolver;

  @Test
  void shouldReturnEmptyDefaultBranchWhenBranchesNotSupported() throws IOException {
    supported(false);

    DefaultBranchResolver.Result result = resolver.resolve();
    assertThat(result.isEmpty()).isFalse();
    assertThat(result.getDefaultBranch()).isEmpty();
  }

  @Test
  void shouldReturnEmptyWithoutBranches() throws IOException {
    supported(true);
    branches();

    DefaultBranchResolver.Result result = resolver.resolve();
    assertThat(result.isEmpty()).isTrue();
    assertThat(result.getDefaultBranch()).isEmpty();
  }

  @Test
  void shouldReturnEmptyWithoutDefaultBranch() throws IOException {
    supported(true);
    branches(
      Branch.normalBranch("develop", "4211", 1L),
      Branch.normalBranch("main", "4211", 1L)
    );

    DefaultBranchResolver.Result result = resolver.resolve();
    assertThat(result.isEmpty()).isTrue();
    assertThat(result.getDefaultBranch()).isEmpty();
  }

  @Test
  void shouldReturnDefaultBranch() throws IOException {
    supported(true);
    branches(
      Branch.defaultBranch("develop", "4211", 1L),
      Branch.normalBranch("main", "4211", 1L)
    );

    DefaultBranchResolver.Result result = resolver.resolve();
    assertThat(result.isEmpty()).isFalse();
    assertThat(result.getDefaultBranch()).contains("develop");
  }

  private void branches(Branch... branches) throws IOException {
    when(repositoryService.getBranchesCommand().getBranches()).thenReturn(new Branches(branches));
  }

  private void supported(boolean isSupported) {
    when(repositoryService.isSupported(Command.BRANCHES)).thenReturn(isSupported);
  }
}
