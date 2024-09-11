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
