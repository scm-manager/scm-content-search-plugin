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
import sonia.scm.repository.Changeset;
import sonia.scm.repository.ChangesetPagingResult;
import sonia.scm.repository.Person;
import sonia.scm.repository.api.LogCommandBuilder;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.spi.LogCommand;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static com.cloudogu.scm.search.DefaultBranchResolver.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LatestRevisionResolverTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private RepositoryService repositoryService;

  @Mock
  private DefaultBranchResolver defaultBranchResolver;

  @InjectMocks
  private LatestRevisionResolver resolver;

  @Test
  void shouldReturnEmptyIfDefaultBranchResolverReturnEmpty() throws IOException {
    when(defaultBranchResolver.resolve()).thenReturn(Result.empty());

    Optional<String> revision = resolver.resolve();
    assertThat(revision).isEmpty();
  }

  @Test
  void shouldReturnEmptyIfNoChangesetCouldBeFound() throws IOException {
    when(defaultBranchResolver.resolve()).thenReturn(Result.notSupported());

    changesets();

    Optional<String> revision = resolver.resolve();
    assertThat(revision).isEmpty();
  }

  @Test
  void shouldUseDefaultBranch() throws IOException {
    when(defaultBranchResolver.resolve()).thenReturn(Result.defaultBranch("develop"));

    LogCommandBuilder logCommand = mock(LogCommandBuilder.class);
    when(
      repositoryService.getLogCommand()
        .setPagingLimit(1)
        .setDisablePreProcessors(true)
        .setDisableCache(true)
    ).thenReturn(logCommand);

    when(logCommand.getChangesets()).thenReturn(new ChangesetPagingResult(0, Collections.emptyList()));

    Optional<String> revision = resolver.resolve();
    assertThat(revision).isEmpty();

    verify(logCommand).setBranch("develop");
  }

  @Test
  void shouldReturnLatestRevision() throws IOException {
    when(defaultBranchResolver.resolve()).thenReturn(Result.notSupported());

    changesets(new Changeset("42", 1L, Person.toPerson("trillian")));

    Optional<String> revision = resolver.resolve();
    assertThat(revision).contains("42");
  }

  private void changesets(Changeset... changesets) throws IOException {
    when(
      repositoryService.getLogCommand()
        .setPagingLimit(1)
        .setDisablePreProcessors(true)
        .setDisableCache(true)
        .getChangesets()
    ).thenReturn(new ChangesetPagingResult(changesets.length, Arrays.asList(changesets)));
  }
}
