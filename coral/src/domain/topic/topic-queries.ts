import { ENVIRONMENT_NOT_INITIALIZED } from "src/domain/environment/environment-types";
import { Team, TEAM_NOT_INITIALIZED } from "src/domain/team";
import { getTopics } from "src/domain/topic/topic-api";

const topicsQuery = ({
  currentPage,
  environment,
  teamName,
  searchTerm,
}: {
  currentPage: number;
  environment: string;
  teamName: Team;
  searchTerm?: string;
}) => {
  return {
    queryKey: ["topics", currentPage, environment, teamName, searchTerm],
    queryFn: () =>
      getTopics({ currentPage, environment, teamName, searchTerm }),
    keepPreviousData: true,
    enabled:
      teamName !== TEAM_NOT_INITIALIZED &&
      environment !== ENVIRONMENT_NOT_INITIALIZED,
  };
};

export { topicsQuery };
