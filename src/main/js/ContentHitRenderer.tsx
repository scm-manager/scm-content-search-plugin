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

import React, { FC } from "react";
import {
  HitProps,
  isValueHitField,
  Notification,
  RepositoryAvatar,
  TextHitField,
  useBooleanHitFieldValue,
  useStringHitFieldValue
} from "@scm-manager/ui-components";
import { Hit as HitType } from "@scm-manager/ui-types";
import styled from "styled-components";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { CardList } from "@scm-manager/ui-layout";

const StyledLink = styled(Link)`
  gap: 0.5rem;
`;

const EllipsizeLeftLink = styled(Link)`
  direction: rtl;
  text-align: left;
`;

const FileContent = styled.div`
  border: var(--scm-border);
  border-radius: 0.25rem;
`;

type SyntaxHighlighting = {
  modes: {
    ace?: string;
    codemirror?: string;
    prism?: string;
  };
};

const ContentMessage: FC = ({ children }) => <pre className="p-4 is-size-7 is-family-primary">{children}</pre>;

const BinaryContent: FC = () => {
  const [t] = useTranslation("plugins");
  return <ContentMessage>{t("scm-content-search-plugin.hit.binary")}</ContentMessage>;
};

const EmptyContent: FC = () => {
  const [t] = useTranslation("plugins");
  return <ContentMessage>{t("scm-content-search-plugin.hit.empty")}</ContentMessage>;
};

const isEmpty = (hit: HitType) => {
  const content = hit.fields["content"];
  return !content || (isValueHitField(content) && content.value === "");
};

const useDeterminedLanguage = (hit: HitType) => {
  const language = useStringHitFieldValue(hit, "codingLanguage");
  const syntaxHighlighting = hit._embedded?.syntaxHighlighting as SyntaxHighlighting;
  if (syntaxHighlighting) {
    return (
      syntaxHighlighting.modes.prism || syntaxHighlighting.modes.codemirror || syntaxHighlighting.modes.ace || language
    );
  }
  return language;
};

const TextContent: FC<HitProps> = ({ hit }) => {
  const language = useDeterminedLanguage(hit);
  if (isEmpty(hit)) {
    return <EmptyContent />;
  } else {
    return (
      <pre>
        <code>
          <TextHitField hit={hit} field="content" truncateValueAt={1024} syntaxHighlightingLanguage={language}>
            <EmptyContent />
          </TextHitField>
        </code>
      </pre>
    );
  }
};

const Content: FC<HitProps> = ({ hit }) => {
  const binary = useBooleanHitFieldValue(hit, "binary");

  if (binary) {
    return <BinaryContent />;
  } else {
    return <TextContent hit={hit} />;
  }
};

const ContentHitRenderer: FC<HitProps> = ({ hit }) => {
  const revision = useStringHitFieldValue(hit, "revision");
  const path = useStringHitFieldValue(hit, "path");

  const repository = hit._embedded?.repository;
  const title = `${repository?.namespace}/${repository?.name}`;

  if (!revision || !path || !repository) {
    return <Notification type="danger">Found incomplete content search result</Notification>;
  }

  return (
    <CardList.Card className="is-full-width is-flex-direction-column" key={title}>
      <CardList.Card.Row>
        <CardList.Card.Title className="is-relative">
          <StyledLink
            to={`/repo/${repository.namespace}/${repository.name}`}
            className="is-flex is-justify-content-flex-start is-align-items-center"
          >
            <RepositoryAvatar repository={repository} size={16} /> {title}
          </StyledLink>
        </CardList.Card.Title>
        <EllipsizeLeftLink
          className="is-ellipsis-overflow is-block"
          to={`/repo/${repository.namespace}/${repository.name}/code/sources/${revision}/${path}`}
        >
          <TextHitField hit={hit} field="path" />
        </EllipsizeLeftLink>
      </CardList.Card.Row>
      <CardList.Card.Row>
        <FileContent className="my-2">
          <Content hit={hit} />
        </FileContent>
      </CardList.Card.Row>
      <CardList.Card.Row className="is-size-7 has-text-secondary">
        Revision:{" "}
        <Link
          className="is-ellipsis-overflow is-relative"
          to={`/repo/${repository.namespace}/${repository.name}/code/sources/${revision}`}
        >
          <TextHitField hit={hit} field="revision" />
        </Link>
      </CardList.Card.Row>
    </CardList.Card>
  );
};

export default ContentHitRenderer;
