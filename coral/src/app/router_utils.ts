import isString from "lodash/isString";

enum Routes {
  TOPICS = "/topics",
  TOPIC_REQUEST = "/topics/request",
  TOPIC_ACL_REQUEST = "/topic/:topicName/subscribe",
  TOPIC_SCHEMA_REQUEST = "/topic/:topicName/request-schema",
  APPROVALS = "/approvals",
}

enum ApprovalsTabEnum {
  TOPICS = "APPROVALS_TAB_ENUM_topics",
  ACLS = "APPROVALS_TAB_ENUM_acls",
  SCHEMAS = "APPROVALS_TAB_ENUM_schemas",
  CONNECTORS = "APPROVALS_TAB_ENUM_connectors",
}

const APPROVALS_TAB_ID_INTO_PATH = {
  [ApprovalsTabEnum.TOPICS]: "topics",
  [ApprovalsTabEnum.ACLS]: "acls",
  [ApprovalsTabEnum.SCHEMAS]: "schemas",
  [ApprovalsTabEnum.CONNECTORS]: "connectors",
} as const;

function isApprovalsTabEnum(value: unknown): value is ApprovalsTabEnum {
  if (isString(value)) {
    return Object.prototype.hasOwnProperty.call(
      APPROVALS_TAB_ID_INTO_PATH,
      value
    );
  }
  return false;
}

export {
  ApprovalsTabEnum,
  Routes,
  APPROVALS_TAB_ID_INTO_PATH,
  isApprovalsTabEnum,
};
