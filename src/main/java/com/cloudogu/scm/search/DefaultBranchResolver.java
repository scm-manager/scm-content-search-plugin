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

import lombok.Getter;
import sonia.scm.repository.Branch;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.RepositoryService;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Optional;

public class DefaultBranchResolver {

  private final RepositoryService repositoryService;

  public DefaultBranchResolver(RepositoryService repositoryService) {
    this.repositoryService = repositoryService;
  }

  public Result resolve() throws IOException {
    if (!repositoryService.isSupported(Command.BRANCHES)) {
      return Result.notSupported();
    } else {
      return repositoryService.getBranchesCommand()
        .getBranches()
        .getBranches()
        .stream()
        .filter(Branch::isDefaultBranch)
        .findFirst()
        .map(Branch::getName)
        .map(Result::defaultBranch)
        .orElseGet(Result::empty);
    }
  }

  @Getter
  public static class Result {

    private final boolean empty;
    @Nullable
    private final String defaultBranch;

    private Result(boolean empty, @Nullable String defaultBranch) {
      this.empty = empty;
      this.defaultBranch = defaultBranch;
    }

    public Optional<String> getDefaultBranch() {
      return Optional.ofNullable(defaultBranch);
    }

    public static Result empty() {
      return new Result(true, null);
    }

    public static Result notSupported() {
      return new Result(false, null);
    }

    public static Result defaultBranch(String branch) {
      return new Result(false, branch);
    }
  }


}
