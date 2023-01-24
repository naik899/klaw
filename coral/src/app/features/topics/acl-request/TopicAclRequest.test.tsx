import { cleanup, screen, waitFor } from "@testing-library/react";
import { waitForElementToBeRemoved } from "@testing-library/react/pure";
import userEvent from "@testing-library/user-event";
import { Route, Routes } from "react-router-dom";
import TopicAclRequest from "src/app/features/topics/acl-request/TopicAclRequest";
import { mockCreateAclRequest } from "src/domain/acl/acl-api-msw";
import {
  getMockedResponseGetClusterInfoFromEnv,
  mockGetClusterInfoFromEnv,
  mockGetEnvironments,
} from "src/domain/environment/environment-api.msw";
import { createMockEnvironmentDTO } from "src/domain/environment/environment-test-helper";
import {
  mockedResponseTopicNames,
  mockedResponseTopicTeamLiteral,
  mockGetTopicNames,
  mockGetTopicTeam,
  mockRequestTopic,
} from "src/domain/topic/topic-api.msw";
import api from "src/services/api";
import { server } from "src/services/api-mocks/server";
import { customRender } from "src/services/test-utils/render-with-wrappers";

const mockedNavigate = jest.fn();
jest.mock("react-router-dom", () => ({
  ...jest.requireActual("react-router-dom"),
  useNavigate: () => mockedNavigate,
}));

const dataSetup = ({ isAivenCluster }: { isAivenCluster: boolean }) => {
  mockGetEnvironments({
    mswInstance: server,
    response: {
      data: [
        createMockEnvironmentDTO({
          name: "TST",
          id: "1",
          maxPartitions: "6",
          maxReplicationFactor: "2",
          defaultPartitions: "3",
          defaultReplicationFactor: "2",
        }),
        createMockEnvironmentDTO({
          name: "DEV",
          id: "2",
          maxPartitions: undefined,
          maxReplicationFactor: undefined,
          defaultPartitions: "2",
          defaultReplicationFactor: "2",
        }),
        createMockEnvironmentDTO({
          name: "PROD",
          id: "3",
          maxPartitions: "16",
          maxReplicationFactor: "3",
          defaultPartitions: "2",
          defaultReplicationFactor: "2",
        }),
      ],
    },
  });
  mockGetTopicNames({
    mswInstance: server,
    response: mockedResponseTopicNames,
  });
  mockGetTopicTeam({
    mswInstance: server,
    response: mockedResponseTopicTeamLiteral,
    topicName: "aivtopic1",
  });
  mockGetClusterInfoFromEnv({
    mswInstance: server,
    response: getMockedResponseGetClusterInfoFromEnv(isAivenCluster),
  });
};

const assertSkeleton = async () => {
  const skeleton = screen.getByTestId("skeleton");
  expect(skeleton).toBeVisible();
  await waitForElementToBeRemoved(skeleton);
};

const selectEnvironment = async () => {
  const environmentField = screen.getByRole("combobox", {
    name: "Select environment *",
  });
  const option = screen.getByRole("option", { name: "TST" });
  await userEvent.selectOptions(environmentField, option);
};

describe("<TopicAclRequest />", () => {
  beforeAll(async () => {
    server.listen();
  });

  afterAll(() => {
    server.close();
  });

  describe("Form states (producer, consumer)", () => {
    beforeEach(() => {
      dataSetup({ isAivenCluster: true });

      customRender(
        <Routes>
          <Route
            path="/topic/:topicName/acl/request"
            element={<TopicAclRequest />}
          />
        </Routes>,
        {
          queryClient: true,
          memoryRouter: true,
          customRoutePath: "/topic/aivtopic1/acl/request",
        }
      );
    });

    afterEach(cleanup);

    it("renders TopicProducerForm by by default", async () => {
      await assertSkeleton();

      const aclProducerTypeInput = screen.getByRole("radio", {
        name: "Producer",
      });
      const aclConsumerTypeInput = screen.getByRole("radio", {
        name: "Consumer",
      });
      // Only rendered in Producer form
      const transactionalIdInput = screen.getByLabelText("Transactional ID");

      expect(aclProducerTypeInput).toBeVisible();
      expect(aclProducerTypeInput).toBeChecked();
      expect(aclConsumerTypeInput).not.toBeChecked();
      expect(transactionalIdInput).toBeVisible();
    });

    it("renders the correct AclIpPrincipleTypeField with Principal option checked when choosing an Aiven cluster environment", async () => {
      await assertSkeleton();

      const principalField = screen.getByRole("radio", {
        name: "Principal",
      });
      const ipField = screen.getByRole("radio", {
        name: "IP",
      });

      expect(principalField).not.toBeEnabled();
      expect(principalField).not.toBeChecked();
      expect(ipField).not.toBeEnabled();
      expect(ipField).not.toBeChecked();

      await selectEnvironment();

      await waitFor(() => {
        expect(principalField).toBeEnabled();
        expect(principalField).toBeChecked();
        expect(ipField).toBeDisabled();
        expect(ipField).not.toBeChecked();
      });

      const principalsField = await screen.findByRole("textbox", {
        name: "SSL DN strings / Usernames *",
      });

      await waitFor(() => {
        expect(principalsField).toBeVisible();
        expect(principalsField).toBeEnabled();
      });
    });

    it("renders the appropriate form when switching between Producer and Consumer ACL types", async () => {
      await assertSkeleton();

      const aclProducerTypeInput = screen.getByRole("radio", {
        name: "Producer",
      });
      const aclConsumerTypeInput = screen.getByRole("radio", {
        name: "Consumer",
      });

      expect(aclConsumerTypeInput).toBeVisible();
      expect(aclConsumerTypeInput).not.toBeChecked();

      await userEvent.click(aclConsumerTypeInput);

      // Only rendered in Consumer form
      const consumerGroupInput = screen.getByRole("textbox", {
        name: "Consumer group *",
      });

      expect(aclConsumerTypeInput).toBeChecked();
      expect(aclProducerTypeInput).not.toBeChecked();
      expect(consumerGroupInput).toBeVisible();
    });
  });

  describe("Form submission (Producer)", () => {
    beforeEach(async () => {
      dataSetup({ isAivenCluster: true });

      customRender(
        <Routes>
          <Route
            path="/topic/:topicName/acl/request"
            element={<TopicAclRequest />}
          />
        </Routes>,
        {
          queryClient: true,
          memoryRouter: true,
          customRoutePath: "/topic/aivtopic1/acl/request",
        }
      );
    });

    afterEach(() => {
      cleanup();
      jest.clearAllMocks();
    });

    describe("when API returns an error", () => {
      beforeEach(async () => {
        mockCreateAclRequest({
          mswInstance: server,
          response: {
            data: { message: "Error message example" },
            status: 400,
          },
        });
      });

      it("renders an error message", async () => {
        const spyPost = jest.spyOn(api, "post");
        await assertSkeleton();
        const submitButton = screen.getByRole("button", { name: "Submit" });

        // Fill form with valid data
        await userEvent.selectOptions(
          screen.getByRole("combobox", {
            name: "Select environment *",
          }),
          "DEV"
        );
        await userEvent.click(screen.getByRole("radio", { name: "Literal" }));
        const principalField = await screen.findByRole("textbox", {
          name: "SSL DN strings / Usernames *",
        });
        await userEvent.type(principalField, "Alice");
        await userEvent.tab();

        await waitFor(() => expect(submitButton).toBeEnabled());
        await userEvent.click(submitButton);

        await waitFor(() => {
          expect(submitButton).toBeDisabled();
        });

        expect(spyPost).toHaveBeenCalledTimes(1);
        expect(spyPost).toHaveBeenCalledWith("/createAcl", {
          remarks: "",
          aclIpPrincipleType: "PRINCIPAL",
          acl_ssl: ["Alice"],
          aclPatternType: "LITERAL",
          topicname: "aivtopic1",
          environment: "2",
          topictype: "Producer",
          transactionalId: "",
          teamname: "Ospo",
        });

        const alert = await screen.findByRole("alert");
        expect(alert).toHaveTextContent("Error message example");
      });
    });

    describe("when API request is successful", () => {
      beforeEach(async () => {
        mockRequestTopic({
          mswInstance: server,
          response: { data: { status: "200 OK" } },
        });
      });

      it("redirects user to myTopicRequests", async () => {
        const spyPost = jest.spyOn(api, "post");
        await assertSkeleton();
        const submitButton = screen.getByRole("button", { name: "Submit" });

        // Fill form with valid data
        await userEvent.selectOptions(
          screen.getByRole("combobox", {
            name: "Select environment *",
          }),
          "DEV"
        );
        await userEvent.click(screen.getByRole("radio", { name: "Literal" }));
        const principalField = await screen.findByRole("textbox", {
          name: "SSL DN strings / Usernames *",
        });
        await userEvent.type(principalField, "Alice");
        await userEvent.tab();

        await waitFor(() => expect(submitButton).toBeEnabled());
        await userEvent.click(submitButton);

        await waitFor(() => {
          expect(submitButton).toBeDisabled();
        });

        expect(spyPost).toHaveBeenCalledTimes(1);
        expect(spyPost).toHaveBeenCalledWith("/createAcl", {
          remarks: "",
          aclIpPrincipleType: "PRINCIPAL",
          acl_ssl: ["Alice"],
          aclPatternType: "LITERAL",
          topicname: "aivtopic1",
          environment: "2",
          topictype: "Producer",
          transactionalId: "",
          teamname: "Ospo",
        });

        await waitFor(() => {
          expect(mockedNavigate).toHaveBeenCalledTimes(1);
          expect(mockedNavigate).toHaveBeenCalledWith(-1);
        });
      });
    });
  });
  describe("Form submission (Consumer)", () => {
    beforeEach(async () => {
      dataSetup({ isAivenCluster: true });

      customRender(
        <Routes>
          <Route
            path="/topic/:topicName/acl/request"
            element={<TopicAclRequest />}
          />
        </Routes>,
        {
          queryClient: true,
          memoryRouter: true,
          customRoutePath: "/topic/aivtopic1/acl/request",
        }
      );
    });

    afterEach(() => {
      cleanup();
      jest.clearAllMocks();
    });

    describe("when API returns an error", () => {
      beforeEach(async () => {
        mockCreateAclRequest({
          mswInstance: server,
          response: {
            data: { message: "Error message example" },
            status: 400,
          },
        });
      });

      it("renders an error message", async () => {
        const spyPost = jest.spyOn(api, "post");
        await assertSkeleton();

        const aclConsumerTypeInput = screen.getByRole("radio", {
          name: "Consumer",
        });
        await userEvent.click(aclConsumerTypeInput);

        const submitButton = screen.getByRole("button", {
          name: "Submit",
        });

        // Fill form with valid data
        await userEvent.selectOptions(
          screen.getByRole("combobox", {
            name: "Select environment *",
          }),
          "DEV"
        );

        const consumerGroupInput = screen.getByRole("textbox", {
          name: "Consumer group *",
        });
        await userEvent.type(consumerGroupInput, "Group");

        const principalField = await screen.findByRole("textbox", {
          name: "SSL DN strings / Usernames *",
        });
        await userEvent.type(principalField, "Alice");
        await userEvent.tab();

        await waitFor(() => expect(submitButton).toBeEnabled());
        await userEvent.click(submitButton);

        await waitFor(() => {
          expect(submitButton).toBeDisabled();
        });

        expect(spyPost).toHaveBeenCalledTimes(1);
        expect(spyPost).toHaveBeenCalledWith("/createAcl", {
          remarks: "",
          aclIpPrincipleType: "PRINCIPAL",
          acl_ssl: ["Alice"],
          aclPatternType: "LITERAL",
          topicname: "aivtopic1",
          environment: "2",
          topictype: "Consumer",
          teamname: "Ospo",
          consumergroup: "Group",
        });

        const alert = await screen.findByRole("alert");
        expect(alert).toHaveTextContent("Error message example");
      });
    });

    describe("when API request is successful", () => {
      beforeEach(async () => {
        mockRequestTopic({
          mswInstance: server,
          response: { data: { status: "200 OK" } },
        });
      });

      it("redirects user to myTopicRequests", async () => {
        const spyPost = jest.spyOn(api, "post");
        await assertSkeleton();

        const aclConsumerTypeInput = screen.getByRole("radio", {
          name: "Consumer",
        });
        await userEvent.click(aclConsumerTypeInput);

        const submitButton = screen.getByRole("button", {
          name: "Submit",
        });

        // Fill form with valid data
        await userEvent.selectOptions(
          screen.getByRole("combobox", {
            name: "Select environment *",
          }),
          "DEV"
        );
        const consumerGroupInput = screen.getByRole("textbox", {
          name: "Consumer group *",
        });
        await userEvent.type(consumerGroupInput, "Group");

        const principalField = await screen.findByRole("textbox", {
          name: "SSL DN strings / Usernames *",
        });
        await userEvent.type(principalField, "Alice");
        await userEvent.tab();

        await waitFor(() => expect(submitButton).toBeEnabled());
        await userEvent.click(submitButton);

        await waitFor(() => {
          expect(submitButton).toBeDisabled();
        });

        expect(spyPost).toHaveBeenCalledTimes(1);
        expect(spyPost).toHaveBeenCalledWith("/createAcl", {
          remarks: "",
          aclIpPrincipleType: "PRINCIPAL",
          acl_ssl: ["Alice"],
          aclPatternType: "LITERAL",
          topicname: "aivtopic1",
          environment: "2",
          topictype: "Consumer",
          teamname: "Ospo",
          consumergroup: "Group",
        });

        await waitFor(() => {
          expect(mockedNavigate).toHaveBeenCalledTimes(1);
          expect(mockedNavigate).toHaveBeenCalledWith(-1);
        });
      });
    });
  });
});
