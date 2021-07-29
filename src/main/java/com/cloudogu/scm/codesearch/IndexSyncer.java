package com.cloudogu.scm.codesearch;

import com.github.legman.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sonia.scm.EagerSingleton;
import sonia.scm.NotFoundException;
import sonia.scm.plugin.Extension;
import sonia.scm.repository.Branch;
import sonia.scm.repository.Changeset;
import sonia.scm.repository.PostReceiveRepositoryHookEvent;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryPermissions;
import sonia.scm.repository.api.Command;
import sonia.scm.repository.api.HookContext;
import sonia.scm.repository.api.RepositoryService;
import sonia.scm.repository.api.RepositoryServiceFactory;
import sonia.scm.search.Id;
import sonia.scm.search.Index;
import sonia.scm.search.IndexNames;
import sonia.scm.search.IndexQueue;

import javax.inject.Inject;
import java.io.IOException;

@Extension
@EagerSingleton
@SuppressWarnings("UnstableApiUsage")
public class IndexSyncer {

  private static final Logger LOG = LoggerFactory.getLogger(IndexSyncer.class);

  private final RepositoryServiceFactory repositoryServiceFactory;
  private final IndexQueue indexQueue;

  @Inject
  public IndexSyncer(RepositoryServiceFactory repositoryServiceFactory, IndexQueue indexQueue) {
    this.repositoryServiceFactory = repositoryServiceFactory;
    this.indexQueue = indexQueue;
  }

  @Subscribe
  public void handle(PostReceiveRepositoryHookEvent event) throws IOException {
    Repository repository = event.getRepository();
    try(
      RepositoryService repositoryService = repositoryServiceFactory.create(repository);
      Index index = indexQueue.getQueuedIndex(IndexNames.DEFAULT)
    ) {
      Syncer syncer = new Syncer(repositoryService, event.getContext(), index);
      if (syncer.shouldProcess()) {
        syncer.sync();
      } else {
        LOG.debug("nothing to sync");
      }
    }
  }

  private static final class Syncer {

    private final RepositoryService repositoryService;
    private final HookContext context;
    private final Index index;

    private String defaultBranch = null;

    public Syncer(RepositoryService repositoryService, HookContext context, Index index) throws IOException {
      this.repositoryService = repositoryService;
      this.context = context;
      this.index = index;

      if (repositoryService.isSupported(Command.BRANCHES)) {
        this.defaultBranch = findDefaultBranch();
      }
    }

    public boolean shouldProcess() {
      return defaultBranch == null || context.getBranchProvider().getCreatedOrModified().contains(defaultBranch);
    }

    public void sync() throws IOException {
      String revision = null;
      PathCollector pathCollector = new PathCollector(repositoryService);
      for (Changeset changeset : context.getChangesetProvider().setDisablePreProcessors(true).getChangesets()) {
        if (shouldIndex(changeset)) {
          pathCollector.collect(changeset);
          revision = changeset.getId();
        }
      }

      Repository repository = repositoryService.getRepository();
      for (String path : pathCollector.getPathToDelete()) {
        Id id = Id.of(defaultBranch, path).withRepository(repository);
        index.delete(id, SourceCode.class);
      }

      String permission = RepositoryPermissions.pull(repository).asShiroString();
      for (String path : pathCollector.getPathToStore()) {
        Id id = Id.of(defaultBranch, path).withRepository(repository);
        index.store(id, permission, create(revision, path));
      }
    }

    private SourceCode create(String revision, String path) throws IOException {
      return new SourceCode(revision, path, content(revision, path));
    }

    private String content(String revision, String path) throws IOException {
      return repositoryService.getCatCommand().setRevision(revision).getContent(path);
    }

    private boolean shouldIndex(Changeset changeset) {
      return defaultBranch == null || changeset.getBranches().contains(defaultBranch);
    }

    private String findDefaultBranch() throws IOException {
      return repositoryService.getBranchesCommand()
        .getBranches()
        .getBranches()
        .stream()
        .filter(Branch::isDefaultBranch)
        .findFirst()
        .map(Branch::getName)
        .orElseThrow(() -> new NotFoundException("branch", "default"));
    }
  }
}
