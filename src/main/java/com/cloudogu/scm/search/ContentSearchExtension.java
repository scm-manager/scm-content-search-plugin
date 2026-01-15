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

import com.cloudogu.mcp.ToolSearchExtension;
import jakarta.inject.Inject;
import sonia.scm.plugin.Extension;
import sonia.scm.plugin.Requires;
import sonia.scm.repository.RepositoryManager;
import sonia.scm.search.Hit;

import java.util.Map;
import java.util.Optional;

@Extension
@Requires("scm-mcp-plugin")
class ContentSearchExtension implements ToolSearchExtension {

  private final RepositoryManager repositoryManager;

  @Inject
  ContentSearchExtension(RepositoryManager repositoryManager) {
    this.repositoryManager = repositoryManager;
  }

  @Override
  public String getSearchType() {
    return "content";
  }

  @Override
  public String getSummary() {
    return "File content within repositories (corresponding type is 'content')";
  }

  @Override
  public String getDescription() {
    return """
      Search fields of type 'content':
      
      1. path - Path of the file within the repository
      2. filename - Name of the file including the extension within the repository
      3. extension - Name of the file extension within the repository
      4. content - Content of the file within the repository

      For the results, an extract of the content of the file where the search term was found can be extracted from the structured content.""";
  }

  @Override
  public String[] tableColumnHeader() {
    return new String[] {"Repository", "Path", "Binary", "Coding Language", "Revision"};
  }

  @Override
  public String[] transformHitToTableFields(Hit hit) {
    return new String[]{
      extractRepository(hit).orElse("unknown"),
      extractValue(hit,"path"),
      extractValue(hit,"binary"),
      extractValue(hit,"codingLanguage"),
      extractValue(hit,"revision")
    };
  }

  @Override
  public Map<String, Object> transformHitToStructuredAnswer(Hit hit) {
    Map<String, Object> result = ToolSearchExtension.super.transformHitToStructuredAnswer(hit);
    extractRepository(hit)
      .ifPresent(repository -> result.put("repository", repository));
    return result;
  }

  private Optional<String> extractRepository(Hit hit) {
    return hit.getRepositoryId()
      .map(
        repositoryId -> repositoryManager.get(repositoryId).getNamespaceAndName().toString()
      );
  }
}
