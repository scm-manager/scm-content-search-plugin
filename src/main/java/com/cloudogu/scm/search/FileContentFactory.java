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

import com.google.common.io.ByteStreams;
import sonia.scm.io.ContentType;
import sonia.scm.io.ContentTypeResolver;
import sonia.scm.repository.api.RepositoryService;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class FileContentFactory {

  private static final int HEAD_BUFFER_SIZE = 1024;

  private final ContentTypeResolver contentTypeResolver;
  private final Set<BinaryFileContentResolver> binaryFileContentResolvers;

  @Inject
  public FileContentFactory(ContentTypeResolver contentTypeResolver, Set<BinaryFileContentResolver> binaryFileContentResolvers) {
    this.contentTypeResolver = contentTypeResolver;
    this.binaryFileContentResolvers = binaryFileContentResolvers;
  }

  public FileContent create(RepositoryService repositoryService, String revision, String path) throws IOException {
    ContentType contentType = contentTypeResolver.resolve(path);
    if (contentType.isText() || isBinaryDefault(contentType)) {
      return create(repositoryService, revision, path, contentType);
    }
    return new FileContent(revision, path, contentType);
  }

  private boolean isBinaryDefault(ContentType contentType) {
    return "application".equals(contentType.getPrimary()) || "octet-stream".equals(contentType.getSecondary());
  }

  private FileContent create(RepositoryService repositoryService, String revision, String path, ContentType contentType) throws IOException {
    try (InputStream content = repositoryService.getCatCommand().setRevision(revision).getStream(path)) {

      byte[] buffer = readHeader(content);
      if (buffer.length > 0) {
        ContentType moreAccurateContentType = contentTypeResolver.resolve(path, buffer);
        if (moreAccurateContentType.isText()) {
          ByteArrayOutputStream output = writeContentToStream(buffer, content);
          return new FileContent(revision, path, moreAccurateContentType, output.toString(StandardCharsets.UTF_8));
        } else {
          ByteArrayOutputStream baos = writeContentToStream(buffer, content);
          for (BinaryFileContentResolver resolver : binaryFileContentResolvers) {
            if (resolver.isSupported(contentType.getRaw())) {
              String fileContent = resolver.resolveContent(new ByteArrayInputStream(baos.toByteArray()));
              return new FileContent(revision, path, moreAccurateContentType, fileContent);
            }
          }
        }
      }
      return new FileContent(revision, path, contentType);
    }
  }

  private ByteArrayOutputStream writeContentToStream(byte[] buffer, InputStream inputStream) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    output.write(buffer);
    ByteStreams.copy(inputStream, output);
    return output;
  }

  private byte[] readHeader(InputStream content) throws IOException {
    byte[] buffer = new byte[HEAD_BUFFER_SIZE];
    int read = content.read(buffer);
    if (read > 0) {
      if (read < HEAD_BUFFER_SIZE) {
        byte[] newBuffer = new byte[read];
        System.arraycopy(buffer, 0, newBuffer, 0, read);
        return newBuffer;
      } else {
        return buffer;
      }
    }
    return new byte[0];
  }
}
