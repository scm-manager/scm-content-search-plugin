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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.io.ContentType;
import sonia.scm.io.ContentTypeResolver;
import sonia.scm.repository.api.RepositoryService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileContentFactoryTest {

  @Mock
  private ContentTypeResolver contentTypeResolver;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private RepositoryService repositoryService;

  @InjectMocks
  private FileContentFactory fileContentFactory;

  @Test
  void shouldCreateBinaryContent() throws IOException {
    ContentType contentType = mockContentType(false, "image", "png");

    when(contentTypeResolver.resolve("alpaka.png")).thenReturn(contentType);

    FileContent content = fileContentFactory.create(repositoryService, "42", "alpaka.png");
    assertThat(content.getRevision()).isEqualTo("42");
    assertThat(content.getPath()).isEqualTo("alpaka.png");
    assertThat(content.getContentType()).isEqualTo("image/png");
    assertThat(content.isBinary()).isTrue();
    assertThat(content.getContent()).isNull();
    assertThat(content.getCodingLanguage()).isNull();
  }

  @Test
  void shouldReturnEmptyWithEmptyBuffer() throws IOException {
    ContentType contentType = mockContentType(true, "application", "octet-stream");
    when(contentTypeResolver.resolve("License.txt")).thenReturn(contentType);
    when(repositoryService.getCatCommand().setRevision("42").getStream("License.txt")).thenReturn(new ByteArrayInputStream(new byte[0]));

    FileContent content = fileContentFactory.create(repositoryService, "42", "License.txt");

    assertThat(content.getRevision()).isEqualTo("42");
    assertThat(content.getPath()).isEqualTo("License.txt");
    assertThat(content.getContentType()).isEqualTo("application/octet-stream");
    assertThat(content.isBinary()).isFalse();
    assertThat(content.getContent()).isNull();
    assertThat(content.getCodingLanguage()).isNull();
  }

  @Test
  void shouldReturnTextContent() throws IOException {
    ContentType contentType = mockContentType(false, "application", "octet-stream");
    when(contentTypeResolver.resolve("App.java")).thenReturn(contentType);

    String contentValue ="public class App {}";
    byte[] bytes = contentValue.getBytes(StandardCharsets.UTF_8);
    InputStream stream = new ByteArrayInputStream(bytes);
    when(repositoryService.getCatCommand().setRevision("42").getStream("App.java")).thenReturn(stream);

    contentType = mockContentType(true, "text", "java", "java");
    when(contentTypeResolver.resolve(eq("App.java"), any())).thenReturn(contentType);

    FileContent content = fileContentFactory.create(repositoryService, "42", "App.java");

    assertThat(content.getRevision()).isEqualTo("42");
    assertThat(content.getPath()).isEqualTo("App.java");
    assertThat(content.getContentType()).isEqualTo("text/java");
    assertThat(content.isBinary()).isFalse();
    assertThat(content.getContent()).isEqualTo(contentValue);
    assertThat(content.getCodingLanguage()).isEqualTo("java");
  }

  @Test
  void shouldReturnBinaryIfMoreAccurateContentTypeIsNoLongerText() throws IOException {
    ContentType contentType = mockContentType(true, "text", "plain");
    when(contentTypeResolver.resolve("bin")).thenReturn(contentType);

    byte[] bytes = new byte[]{ 0xc, 0xa, 0xf, 0xe };
    InputStream stream = new ByteArrayInputStream(bytes);
    when(repositoryService.getCatCommand().setRevision("21").getStream("bin")).thenReturn(stream);

    contentType = mockContentType(false, "application", "octet-stream");
    when(contentTypeResolver.resolve(eq("bin"), any())).thenReturn(contentType);

    FileContent content = fileContentFactory.create(repositoryService, "21", "bin");

    assertThat(content.getRevision()).isEqualTo("21");
    assertThat(content.getPath()).isEqualTo("bin");
    assertThat(content.getContentType()).isEqualTo("application/octet-stream");
    assertThat(content.isBinary()).isTrue();
    assertThat(content.getContent()).isNull();
    assertThat(content.getCodingLanguage()).isNull();
  }

  @Nested
  class FilenameAndExtensionTests {

    @BeforeEach
    void setUp() {
      ContentType contentType = mockContentType(true, "application", "octet-stream");
      when(contentTypeResolver.resolve(any())).thenReturn(contentType);
    }

    @Test
    void shouldStoreFileName() throws IOException {
      FileContent content = fileContentFactory.create(repositoryService, "42", "Dockerfile");
      assertThat(content.getFilename()).isEqualTo("Dockerfile");
    }

    @Test
    void shouldExtractFileNameFromPath() throws IOException {
      FileContent content = fileContentFactory.create(repositoryService, "21", "scm-plugins/scm-git-plugin/README");
      assertThat(content.getFilename()).isEqualTo("README");
    }

    @Test
    void shouldExtractFileExtension() throws IOException {
      FileContent content = fileContentFactory.create(repositoryService, "21", "build.gradle");
      assertThat(content.getExtension()).isEqualTo("gradle");
    }

    @Test
    void shouldReturnNullForFilesWithoutExtension() throws IOException {
      FileContent content = fileContentFactory.create(repositoryService, "21", "Vagrantfile");
      assertThat(content.getExtension()).isNull();
    }

  }

  private ContentType mockContentType(boolean isText, String primary, String secondary) {
    return mockContentType(isText, primary, secondary, null);
  }

  private ContentType mockContentType(boolean isText, String primary, String secondary, String language) {
    ContentType contentType = mock(ContentType.class);
    lenient().when(contentType.isText()).thenReturn(isText);
    lenient().when(contentType.getRaw()).thenReturn(primary + "/" + secondary);
    lenient().when(contentType.getPrimary()).thenReturn(primary);
    lenient().when(contentType.getSecondary()).thenReturn(secondary);
    lenient().when(contentType.getLanguage()).thenReturn(Optional.ofNullable(language));
    return contentType;
  }
}
