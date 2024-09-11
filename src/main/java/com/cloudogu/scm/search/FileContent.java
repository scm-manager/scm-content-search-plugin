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

import com.google.common.base.Strings;
import com.google.common.io.Files;
import lombok.Getter;
import sonia.scm.io.ContentType;
import sonia.scm.search.Indexed;
import sonia.scm.search.IndexedType;

import javax.annotation.Nullable;
import java.nio.file.Paths;

@Getter
@IndexedType(value = "content", repositoryScoped = true, namespaceScoped = true)
@SuppressWarnings("UnstableApiUsage")
public class FileContent {

  static final int VERSION = 2;

  @Indexed(type = Indexed.Type.STORED_ONLY)
  private final String revision;

  @Indexed(
    defaultQuery = true,
    boost = 1.5f,
    analyzer = Indexed.Analyzer.PATH
  )
  private final String path;

  @Indexed(analyzer = Indexed.Analyzer.PATH)
  private final String filename;

  @Nullable
  @Indexed(type = Indexed.Type.SEARCHABLE)
  private final String extension;

  @Nullable
  @Indexed(
    defaultQuery = true,
    highlighted = true,
    analyzer = Indexed.Analyzer.CODE
  )
  private final String content;

  @Indexed(type = Indexed.Type.STORED_ONLY)
  private final String contentType;

  @Indexed(type = Indexed.Type.STORED_ONLY)
  private final boolean binary;

  @Nullable
  @Indexed(type = Indexed.Type.STORED_ONLY)
  private final String codingLanguage;

  public FileContent(String revision, String path, ContentType contentType) {
    this(revision, path, contentType, null);
  }

  public FileContent(String revision, String path, ContentType contentType, @Nullable String content) {
    this.revision = revision;
    this.path = path;
    this.filename = fileName(path);
    this.extension = extension(filename);
    this.contentType = contentType.getRaw();
    this.binary = !contentType.isText();
    this.codingLanguage = contentType.getLanguage().orElse(null);
    this.content = content;
  }

  private String fileName(String path) {
    return Paths.get(path).getFileName().toString();
  }

  private String extension(String fileName) {
    return Strings.emptyToNull(Files.getFileExtension(fileName));
  }

}
