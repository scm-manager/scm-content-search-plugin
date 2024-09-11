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

import sonia.scm.repository.Added;
import sonia.scm.repository.Copied;
import sonia.scm.repository.Modifications;
import sonia.scm.repository.Modified;
import sonia.scm.repository.Removed;
import sonia.scm.repository.Renamed;
import sonia.scm.repository.api.RepositoryService;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UpdatePathCollector implements PathCollector {

  private final Set<String> pathToStore = new HashSet<>();
  private final Set<String> pathToDelete = new HashSet<>();
  private final RepositoryService repositoryService;

  public UpdatePathCollector(RepositoryService repositoryService) {
    this.repositoryService = repositoryService;
  }

  @Override
  public Collection<String> getPathToStore() {
    return pathToStore;
  }

  @Override
  public Collection<String> getPathToDelete() {
    return pathToDelete;
  }

  void collect(String from, String to) throws IOException {
    Modifications modifications = repositoryService.getModificationsCommand()
      .baseRevision(from)
      .revision(to)
      .disablePreProcessors(true)
      .disableCache(true)
      .getModifications();

    collect(modifications);
  }

  private void collect(Modifications modifications) {
    added(modifications.getAdded());
    modified(modifications.getModified());
    copied(modifications.getCopied());
    removed(modifications.getRemoved());
    renamed(modifications.getRenamed());
  }

  private void added(List<Added> modifications) {
    for (Added modification : modifications) {
      store(modification.getPath());
    }
  }

  private void modified(List<Modified> modifications) {
    for (Modified modification : modifications) {
      store(modification.getPath());
    }
  }

  private void copied(List<Copied> modifications) {
    for (Copied modification : modifications) {
      store(modification.getTargetPath());
    }
  }

  private void removed(List<Removed> modifications) {
    for (Removed modification : modifications) {
      delete(modification.getPath());
    }
  }

  private void renamed(List<Renamed> modifications) {
    for (Renamed modification : modifications) {
      store(modification.getNewPath());
      delete(modification.getOldPath());
    }
  }

  private void store(String path) {
    pathToStore.add(path);
  }

  private void delete(String path) {
    pathToDelete.add(path);
  }

}
