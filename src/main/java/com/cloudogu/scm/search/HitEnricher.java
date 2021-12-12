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

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import de.otto.edison.hal.HalRepresentation;
import sonia.scm.api.v2.resources.Enrich;
import sonia.scm.api.v2.resources.HalAppender;
import sonia.scm.api.v2.resources.HalEnricher;
import sonia.scm.api.v2.resources.HalEnricherContext;
import sonia.scm.io.ContentTypeResolver;
import sonia.scm.plugin.Extension;
import sonia.scm.search.Hit;

import javax.inject.Inject;
import java.util.Map;

@Extension
@Enrich(Hit.class)
@SuppressWarnings("UnstableApiUsage")
public class HitEnricher implements HalEnricher {

  private final ContentTypeResolver contentTypeResolver;

  @Inject
  public HitEnricher(ContentTypeResolver contentTypeResolver) {
    this.contentTypeResolver = contentTypeResolver;
  }

  @Override
  public void enrich(HalEnricherContext context, HalAppender appender) {
    Hit hit = context.oneRequireByType(Hit.class);
    Map<String, Hit.Field> fields = hit.getFields();

    Hit.Field field = fields.get("codingLanguage");
    if (field instanceof Hit.ValueField) {
      Object value = ((Hit.ValueField) field).getValue();
      if (value instanceof String) {
        String language = (String) value;

        Map<String, String> syntaxModesByLanguage = contentTypeResolver.findSyntaxModesByLanguage(language);
        if (!syntaxModesByLanguage.isEmpty()) {
          appender.appendEmbedded("syntaxHighlighting", new SyntaxHighlighting(syntaxModesByLanguage));
        }
      }
    }
  }

  public static class SyntaxHighlighting extends HalRepresentation {

    private final Map<String, String> modes;

    public SyntaxHighlighting(Map<String, String> modes) {
      this.modes = modes;
    }

    public Map<String, String> getModes() {
      return modes;
    }
  }

}
