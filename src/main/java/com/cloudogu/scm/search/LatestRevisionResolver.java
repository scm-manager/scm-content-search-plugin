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
