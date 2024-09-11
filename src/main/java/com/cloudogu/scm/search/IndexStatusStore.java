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

import sonia.scm.repository.Repository;
import sonia.scm.store.DataStore;
import sonia.scm.store.DataStoreFactory;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Instant;
import java.util.Optional;

import static com.cloudogu.scm.search.IndexStatus.EMPTY;

@Singleton
public class IndexStatusStore {

  private static final String STORE_NAME ="content-search-status";

  private final DataStore<IndexStatus> store;

  @Inject
  public IndexStatusStore(DataStoreFactory storeFactory) {
    this.store = storeFactory.withType(IndexStatus.class).withName(STORE_NAME).build();
  }

  public void empty(Repository repository) {
    update(repository, EMPTY);
  }

  private IndexStatus status(String revision) {
    return new IndexStatus(revision, Instant.now(), FileContent.VERSION);
  }

  public void update(Repository repository, String revision) {
    store.put(repository.getId(), status(revision));
  }

  void update(Repository repository, String revision, int version) {
    IndexStatus status = status(revision);
    status.setVersion(version);
    store.put(repository.getId(), status);
  }

  public Optional<IndexStatus> get(Repository repository) {
    return store.getOptional(repository.getId());
  }

}

