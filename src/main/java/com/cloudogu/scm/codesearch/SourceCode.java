package com.cloudogu.scm.codesearch;

import lombok.Value;
import sonia.scm.search.Indexed;
import sonia.scm.search.IndexedType;

@Value
@IndexedType
@SuppressWarnings("UnstableApiUsage")
public class SourceCode {

  @Indexed(type = Indexed.Type.STORED_ONLY)
  String revision;
  @Indexed(defaultQuery = true, boost = 1.5f)
  String path;
  @Indexed(defaultQuery = true, highlighted = true)
  String content;

}
