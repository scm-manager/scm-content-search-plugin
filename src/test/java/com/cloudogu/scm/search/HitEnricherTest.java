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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.api.v2.resources.HalAppender;
import sonia.scm.api.v2.resources.HalEnricherContext;
import sonia.scm.io.ContentTypeResolver;
import sonia.scm.search.Hit;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@SuppressWarnings("UnstableApiUsage")
@ExtendWith(MockitoExtension.class)
class HitEnricherTest {

  @Mock
  private ContentTypeResolver contentTypeResolver;
  @Mock
  private HalEnricherContext context;
  @Mock
  private HalAppender appender;

  @InjectMocks
  private HitEnricher enricher;

  @BeforeEach
  void setUp() {
    Map<String, String> syntaxModesByLanguage = new HashMap<>();
    syntaxModesByLanguage.put("ace", "go-ace");
    syntaxModesByLanguage.put("codemirror", "go-codemirror");
    syntaxModesByLanguage.put("prism", "go-prism");
    lenient().when(contentTypeResolver.findSyntaxModesByLanguage("go")).thenReturn(syntaxModesByLanguage);
  }

  @Test
  void shouldNotEnrichWithoutCodingLanguage() {
    setUpHalContext(Collections.emptyMap());

    enricher.enrich(context, appender);

    verifyNoMoreInteractions(appender);
  }

  @Test
  void shouldNotEnrichIfCodingLanguageIsNoValueField() {
    Map<String, Hit.Field> fields = new HashMap<>();
    fields.put("codingLanguage", new Hit.HighlightedField(new String[]{}));
    setUpHalContext(fields);

    enricher.enrich(context, appender);

    verifyNoMoreInteractions(appender);
  }

  @Test
  void shouldNotEnrichIfCodingLanguageValueFieldIsNoString() {
    Map<String, Hit.Field> fields = new HashMap<>();
    fields.put("codingLanguage", new Hit.ValueField(42));
    setUpHalContext(fields);

    enricher.enrich(context, appender);

    verifyNoMoreInteractions(appender);
  }

  @Test
  void shouldNotEnrichIfCodingLanguageIsNotSupported() {
    Map<String, Hit.Field> fields = new HashMap<>();
    fields.put("codingLanguage", new Hit.ValueField("unknown"));
    setUpHalContext(fields);

    enricher.enrich(context, appender);

    verifyNoMoreInteractions(appender);
  }

  @Test
  void shouldEnrich() {
    Map<String, Hit.Field> fields = new HashMap<>();
    fields.put("codingLanguage", new Hit.ValueField("go"));
    setUpHalContext(fields);

    enricher.enrich(context, appender);

    ArgumentCaptor<HitEnricher.SyntaxHighlighting> argumentCaptor = ArgumentCaptor.forClass(HitEnricher.SyntaxHighlighting.class);
    verify(appender).appendEmbedded(Mockito.eq("syntaxHighlighting"), argumentCaptor.capture());

    assertThat(argumentCaptor.getValue().getModes())
      .containsEntry("ace", "go-ace")
      .containsEntry("codemirror", "go-codemirror")
      .containsEntry("prism", "go-prism");
  }

  private void setUpHalContext(Map<String, Hit.Field> fields) {
    Hit hit = new Hit("1", "1", 2.5f, fields);
    doReturn(hit).when(context).oneRequireByType(Hit.class);
  }
}
