import { ALL_ENVIRONMENTS_VALUE } from "src/domain/environment";
import { Team } from "src/domain/team";
import { ALL_TEAMS_VALUE } from "src/domain/team/team-types";
import {
  transformgetTopicAdvancedConfigOptionsResponse,
  transformTopicApiResponse,
} from "src/domain/topic/topic-transformer";
import {
  TopicAdvancedConfigurationOptions,
  TopicApiResponse,
} from "src/domain/topic/topic-types";
import api from "src/services/api";
import { KlawApiRequest, KlawApiResponse } from "types/utils";

const getTopics = async ({
  currentPage = 1,
  environment = "ALL",
  teamName,
  searchTerm,
}: {
  currentPage: number;
  environment: string;
  teamName: Team;
  searchTerm?: string;
}): Promise<TopicApiResponse> => {
  // "ALL_TEAMS_VALUE" represents topic list without
  // the optional team parameter
  // where we still need a way to represent an
  // option for "Select all teams" to users
  const team = teamName !== ALL_TEAMS_VALUE && teamName;

  const params: Record<string, string> = {
    pageNo: currentPage.toString(),
    env: environment || ALL_ENVIRONMENTS_VALUE,
    ...(team && { teamName: team }),
    ...(searchTerm && { topicnamesearch: searchTerm }),
  };

  return api
    .get<KlawApiResponse<"topicsGet">>(
      `/getTopics?${new URLSearchParams(params)}`
    )
    .then(transformTopicApiResponse);
};

type GetTopicNamesArgs = Partial<{
  onlyMyTeamTopics: boolean;
  envSelected?: string;
}>;

const getTopicNames = async ({
  onlyMyTeamTopics,
  envSelected,
}: GetTopicNamesArgs = {}) => {
  const isMyTeamTopics = onlyMyTeamTopics ?? false;
  const params = {
    isMyTeamTopics: isMyTeamTopics.toString(),
    ...(envSelected && { envSelected }),
  };

  return api.get<KlawApiResponse<"topicsGetOnly">>(
    `/getTopicsOnly?${new URLSearchParams(params)}`
  );
};

interface GetTopicTeamArgs {
  topicName: string;
  patternType?: "LITERAL" | "PREFIXED";
}

const getTopicTeam = async ({
  topicName,
  patternType = "LITERAL",
}: GetTopicTeamArgs) => {
  const params = { topicName, patternType };

  return api.get<KlawApiResponse<"topicGetTeam">>(
    `/getTopicTeam?${new URLSearchParams(params)}`
  );
};

const getTopicAdvancedConfigOptions = (): Promise<
  TopicAdvancedConfigurationOptions[]
> =>
  api
    .get<KlawApiResponse<"topicAdvancedConfigGet">>("/getAdvancedTopicConfigs")
    .then(transformgetTopicAdvancedConfigOptionsResponse);

const requestTopic = (
  payload: KlawApiRequest<"topicCreate">
): Promise<unknown> => {
  return api.post<
    KlawApiResponse<"topicCreate">,
    KlawApiRequest<"topicCreate">
  >("/createTopics", payload);
};

export {
  getTopics,
  getTopicNames,
  getTopicTeam,
  getTopicAdvancedConfigOptions,
  requestTopic,
};
