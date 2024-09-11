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

import sonia.scm.repository.BrowserResult;
import sonia.scm.repository.FileObject;
import sonia.scm.repository.api.RepositoryService;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class RevisionPathCollector implements PathCollector {

  private final Set<String> pathToStore = new HashSet<>();
  private final RepositoryService repositoryService;

  public RevisionPathCollector(RepositoryService repositoryService) {
    this.repositoryService = repositoryService;
  }

  @Override
  public Collection<String> getPathToDelete() {
    return Collections.emptySet();
  }

  @Override
  public Collection<String> getPathToStore() {
    return pathToStore;
  }

  public void collect(String revision) throws IOException {
    BrowserResult result = repositoryService.getBrowseCommand()
      .setDisableSubRepositoryDetection(true)
      .setDisableLastCommit(true)
      .setDisablePreProcessors(true)
      .setLimit(Integer.MAX_VALUE)
      .setRecursive(true)
      .setRevision(revision)
      .getBrowserResult();

    collect(result.getFile());
  }

  private void collect(FileObject file) {
    if (file.isDirectory()) {
      for (FileObject child : file.getChildren()) {
        collect(child);
      }
    } else {
      pathToStore.add(file.getPath());
    }
  }
}
