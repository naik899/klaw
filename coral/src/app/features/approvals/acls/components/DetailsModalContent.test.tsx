import { cleanup, render, screen } from "@testing-library/react";
import DetailsModalContent from "src/app/features/approvals/acls/components/DetailsModalContent";
import { AclRequest } from "src/domain/acl/acl-types";

const mockedIpsAclRequest: AclRequest = {
  remarks: "hello",
  consumergroup: undefined,
  acl_ip: ["3.3.3.32", "3.3.3.33"],
  acl_ssl: ["User:*"],
  aclPatternType: "PREFIXED",
  transactionalId: undefined,
  req_no: 1015,
  topicname: "newaudittopic",
  environment: "2",
  teamname: "Ospo",
  topictype: "Producer",
  aclIpPrincipleType: "IP_ADDRESS",
  environmentName: "TST",
  teamId: 1003,
  requestingteam: 1003,
  requestingTeamName: "Ospo",
  appname: "App",
  username: "amathieu",
  requesttime: "2023-01-10T13:19:10.757+00:00",
  requesttimestring: "10-Jan-2023 13:19:10",
  aclstatus: "created",
  approver: undefined,
  approvingtime: undefined,
  aclType: "Producer",
  aclResourceType: undefined,
  currentPage: "1",
  otherParams: undefined,
  totalNoPages: "2",
  allPageNos: ["1", ">", ">>"],
  approvingTeamDetails:
    "Team : Ospo, Users : muralibasani,josepprat,samulisuortti,mirjamaulbach,smustafa,aindriul,",
};

const mockedPrincipalsAclrequest: AclRequest = {
  remarks: undefined,
  consumergroup: "-na-",
  acl_ip: undefined,
  acl_ssl: ["mbasani", "maulbach"],
  aclPatternType: "LITERAL",
  transactionalId: undefined,
  req_no: 1014,
  topicname: "aivtopic1",
  environment: "1",
  teamname: "Ospo",
  topictype: "Consumer",
  aclIpPrincipleType: "PRINCIPAL",
  environmentName: "DEV",
  teamId: 1003,
  requestingteam: 1003,
  requestingTeamName: "Ospo",
  appname: "App",
  username: "amathieu",
  requesttime: "2023-01-06T14:50:37.912+00:00",
  requesttimestring: "06-Jan-2023 14:50:37",
  aclstatus: "created",
  approver: undefined,
  approvingtime: undefined,
  aclType: "Consumer",
  aclResourceType: undefined,
  currentPage: "1",
  otherParams: undefined,
  totalNoPages: "2",
  allPageNos: ["1", ">", ">>"],
  approvingTeamDetails:
    "Team : Ospo, Users : muralibasani,josepprat,samulisuortti,mirjamaulbach,smustafa,aindriul,",
};

const findDefinition = (term?: string) => {
  return screen
    .getAllByRole("definition")
    .find((value) => value.textContent === term);
};

const findTerm = (term: string) => {
  return screen
    .getAllByRole("term")
    .find((value) => value.textContent === term);
};

describe("DetailsModalContent", () => {
  describe("renders correct content for ACL request (IPs, Producer, Prefixed)", () => {
    beforeAll(() => {
      render(<DetailsModalContent aclRequest={mockedIpsAclRequest} />);
    });
    afterAll(cleanup);

    it("renders ACL type", () => {
      expect(findTerm("ACL type")).toBeVisible();
      expect(findDefinition(mockedIpsAclRequest.topictype)).toBeVisible();
    });
    it("renders Requesting team", () => {
      expect(findTerm("Requesting team")).toBeVisible();
      expect(
        findDefinition(mockedIpsAclRequest.requestingTeamName)
      ).toBeVisible();
    });
    it("renders Topic", () => {
      expect(findTerm("Topic")).toBeVisible();
      expect(findDefinition(mockedIpsAclRequest.topicname)).toBeVisible();
    });
    it("renders Principals/Username", () => {
      expect(findTerm("Principals/Usernames")).toBeVisible();
      expect(findDefinition("User:* ")).toBeVisible();
    });
    it("renders IP addresses", () => {
      expect(findTerm("IP addresses")).toBeVisible();
      expect(findDefinition("3.3.3.32 ")).toBeVisible();
      expect(findDefinition("3.3.3.33 ")).toBeVisible();
    });
    it("renders Consumer group default text", () => {
      expect(findTerm("Consumer group")).toBeVisible();
      expect(findDefinition("Not applicable")).toBeVisible();
    });
    it("renders Message for the approver", () => {
      expect(findTerm("Message for the approver")).toBeVisible();
      expect(findDefinition(mockedIpsAclRequest.remarks)).toBeVisible();
    });
    it("renders Requested by", () => {
      expect(findTerm("Requested by")).toBeVisible();
      expect(findDefinition(mockedIpsAclRequest.username)).toBeVisible();
    });
    it("renders Requested on", () => {
      expect(findTerm("Requested on")).toBeVisible();
      expect(
        findDefinition(`${mockedIpsAclRequest.requesttimestring} UTC`)
      ).toBeVisible();
    });
  });

  describe("renders correct content for ACL request with Principals (Principals, Consumer, Literal", () => {
    beforeAll(() => {
      render(<DetailsModalContent aclRequest={mockedPrincipalsAclrequest} />);
    });
    afterAll(cleanup);

    it("renders ACL type", () => {
      expect(findTerm("ACL type")).toBeVisible();
      expect(
        findDefinition(mockedPrincipalsAclrequest.topictype)
      ).toBeVisible();
    });
    it("renders Requesting team", () => {
      expect(findTerm("Requesting team")).toBeVisible();
      expect(
        findDefinition(mockedPrincipalsAclrequest.requestingTeamName)
      ).toBeVisible();
    });
    it("renders Topic", () => {
      expect(findTerm("Topic")).toBeVisible();
      expect(
        findDefinition(mockedPrincipalsAclrequest.topicname)
      ).toBeVisible();
    });
    it("renders Principals/Username", () => {
      expect(findTerm("Principals/Usernames")).toBeVisible();
      expect(findDefinition("mbasani ")).toBeVisible();
      expect(findDefinition("maulbach ")).toBeVisible();
    });
    it("does not renders IP addresses", () => {
      expect(findTerm("IP addresses")).toBeUndefined();
    });
    it("renders Consumer group", () => {
      expect(findTerm("Consumer group")).toBeVisible();
      expect(
        findDefinition(mockedPrincipalsAclrequest.consumergroup)
      ).toBeVisible();
    });
    it("renders Message for the approver default text", () => {
      expect(findTerm("Message for the approver")).toBeVisible();
      expect(findDefinition("No message")).toBeVisible();
    });
    it("renders Requested by", () => {
      expect(findTerm("Requested by")).toBeVisible();
      expect(findDefinition(mockedPrincipalsAclrequest.username)).toBeVisible();
    });
    it("renders Requested on", () => {
      expect(findTerm("Requested on")).toBeVisible();
      expect(
        findDefinition(`${mockedPrincipalsAclrequest.requesttimestring} UTC`)
      ).toBeVisible();
    });
  });
});
