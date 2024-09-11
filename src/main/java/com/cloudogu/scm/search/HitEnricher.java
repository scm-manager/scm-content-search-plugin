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

import de.otto.edison.hal.HalRepresentation;
import sonia.scm.api.v2.resources.Enrich;
import sonia.scm.api.v2.resources.HalAppender;
import sonia.scm.api.v2.resources.HalEnricher;
import sonia.scm.api.v2.resources.HalEnricherContext;
import sonia.scm.io.ContentTypeResolver;
import sonia.scm.plugin.Extension;
import sonia.scm.search.Hit;

import jakarta.inject.Inject;
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

  @SuppressWarnings("java:S2160") // no need to override equals for dto
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
