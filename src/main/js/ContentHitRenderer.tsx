import React, {FC} from "react";
import {
  Hit,
  HitProps,
  TextHitField,
  Notification,
  useStringHitFieldValue,
  RepositoryAvatar
} from "@scm-manager/ui-components";
import styled from "styled-components";
import {Link} from "react-router-dom";
import {Repository} from "@scm-manager/ui-types";

const Container = styled(Hit.Content)`
  overflow-x: scroll;
`;

const FileContent = styled.pre`
  border: 1px solid #dbdbdb;
  border-radius: .25rem;
`;

// http://localhost:8081/scm/repo/sourcecode/scm-cas-plugin/code/sources/470d8b1639b81cdb16f176616af8bb37f9b0fa4c/src/

const ContentHitRenderer: FC<HitProps> = ({hit}) => {
  const revision = useStringHitFieldValue(hit, "revision");
  const path = useStringHitFieldValue(hit, "path");
  const repository = hit._embedded?.repository as Repository | undefined;

  if (!revision || !path || !repository) {
    return <Notification type="danger">Found incomplete content search result</Notification>
  }

  return (
    <Hit>
      <Container>
        <div className="is-flex">
          <RepositoryAvatar repository={repository} size={48}/>
          <div className="ml-2">
            <Link to={`/repo/${repository.namespace}/${repository.name}`}>
              <Hit.Title>
                {repository.namespace}/{repository.name}
              </Hit.Title>
            </Link>
            <Link className="is-ellipsis-overflow"
                  to={`/repo/${repository.namespace}/${repository.name}/code/sources/${revision}/${path}`}>
              <TextHitField hit={hit} field="path"/>
            </Link>
          </div>
        </div>
        <FileContent className="my-2">
          <code>
            <TextHitField hit={hit} field="content" truncateValueAt={1024}/>
          </code>
        </FileContent>
        <small className="is-size-7">
          Revision: <Link className="is-ellipsis-overflow"
                          to={`/repo/${repository.namespace}/${repository.name}/code/sources/${revision}`}>
          <TextHitField hit={hit} field="revision"/>
        </Link>
        </small>
      </Container>
    </Hit>
  )
};

export default ContentHitRenderer;
