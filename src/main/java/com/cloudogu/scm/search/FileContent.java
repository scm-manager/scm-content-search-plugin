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

import com.google.common.base.Strings;
import com.google.common.io.Files;
import lombok.Getter;
import sonia.scm.io.ContentType;
import sonia.scm.search.Indexed;
import sonia.scm.search.IndexedType;

import javax.annotation.Nullable;
import java.nio.file.Paths;

@Getter
@IndexedType("content")
@SuppressWarnings("UnstableApiUsage")
public class FileContent {

  static final int VERSION = 1;

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
