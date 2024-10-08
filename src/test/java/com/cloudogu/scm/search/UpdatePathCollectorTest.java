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
