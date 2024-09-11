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

import sonia.scm.repository.Changeset;
import sonia.scm.repository.api.LogCommandBuilder;
import sonia.scm.repository.api.RepositoryService;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class LatestRevisionResolver {

  private final RepositoryService repositoryService;
  private final DefaultBranchResolver defaultBranchResolver;

  public LatestRevisionResolver(RepositoryService repositoryService, DefaultBranchResolver defaultBranchResolver) {
    this.repositoryService = repositoryService;
    this.defaultBranchResolver = defaultBranchResolver;
  }

  public Optional<String> resolve() throws IOException {
    DefaultBranchResolver.Result result = defaultBranchResolver.resolve();
    if (result.isEmpty()) {
      return Optional.empty();
    }

    LogCommandBuilder logCommand = repositoryService.getLogCommand()
      .setPagingLimit(1)
      .setDisablePreProcessors(true)
      .setDisableCache(true);

    result.getDefaultBranch().ifPresent(logCommand::setBranch);

    List<Changeset> changesets = logCommand.getChangesets().getChangesets();
    if (changesets.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(changesets.get(0).getId());
  }

}
