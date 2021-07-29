package com.cloudogu.scm.codesearch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.repository.Added;
import sonia.scm.repository.Changeset;
import sonia.scm.repository.Copied;
import sonia.scm.repository.Modifications;
import sonia.scm.repository.Modified;
import sonia.scm.repository.Removed;
import sonia.scm.repository.Renamed;
import sonia.scm.repository.api.RepositoryService;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PathCollector {

  private static final Logger LOG = LoggerFactory.getLogger(PathCollector.class);

  private final Set<String> pathToStore = new HashSet<>();
  private final Set<String> pathToDelete = new HashSet<>();

  private final RepositoryService repositoryService;

  public PathCollector(RepositoryService repositoryService) {
    this.repositoryService = repositoryService;
  }

  public Set<String> getPathToStore() {
    return pathToStore;
  }

  public Set<String> getPathToDelete() {
    return pathToDelete;
  }

  void collect(Changeset changeset) throws IOException {
    Modifications modifications = repositoryService.getModificationsCommand()
      .revision(changeset.getId())
      .disablePreProcessors(true)
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
    append(pathToStore, path);
  }

  private void delete(String path) {
    append(pathToDelete, path);
  }

  private void append(Set<String> paths, String path) {
    if (isText(path)) {
      paths.add(path);
    } else {
      LOG.trace("skip path {}, because it is no text", path);
    }
  }

  private boolean isText(String path) {
    // TODO move content type resolver to core
    return true;
  }

}
