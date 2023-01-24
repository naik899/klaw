import { Box } from "@aivenio/aquarium";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useForm } from "src/app/components/Form";
import AclTypeField from "src/app/features/topics/acl-request/fields/AclTypeField";
import SkeletonForm from "src/app/features/topics/acl-request/forms/SkeletonForm";
import TopicConsumerForm from "src/app/features/topics/acl-request/forms/TopicConsumerForm";
import TopicProducerForm from "src/app/features/topics/acl-request/forms/TopicProducerForm";
import topicConsumerFormSchema, {
  TopicConsumerFormSchema,
} from "src/app/features/topics/acl-request/schemas/topic-acl-request-consumer";
import topicProducerFormSchema, {
  TopicProducerFormSchema,
} from "src/app/features/topics/acl-request/schemas/topic-acl-request-producer";
import {
  ClusterInfo,
  Environment,
  getEnvironments,
} from "src/domain/environment";
import { getClusterInfo } from "src/domain/environment/environment-api";
import { getTopicTeam, TopicNames, TopicTeam } from "src/domain/topic";
import { topicNamesQuery } from "src/domain/topic/topic-queries";

const TopicAclRequest = () => {
  const { topicName = "" } = useParams();
  const navigate = useNavigate();
  const [topicType, setTopicType] = useState("Producer");

  const topicProducerForm = useForm<TopicProducerFormSchema>({
    schema: topicProducerFormSchema,
    defaultValues: {
      topicname: topicName,
      environment: "placeholder",
      topictype: "Producer",
    },
  });

  const topicConsumerForm = useForm<TopicConsumerFormSchema>({
    schema: topicConsumerFormSchema,
    defaultValues: {
      aclPatternType: "LITERAL",
      topicname: topicName,
      environment: "placeholder",
      topictype: "Consumer",
    },
  });

  const { data: topicNames } = useQuery<TopicNames, Error>(["topic-names"], {
    ...topicNamesQuery(),
    onSuccess: (data) => {
      if (data?.includes(topicName)) {
        return;
      }
      // Navigate back to Topics when topicName does not exist in the topics list
      navigate("/topics");
    },
    enabled: topicName !== "",
  });

  const { data: environments } = useQuery<Environment[], Error>({
    queryKey: ["topic-environments"],
    queryFn: getEnvironments,
  });

  const selectedPatternType =
    topicType === "Producer"
      ? topicProducerForm.watch("aclPatternType")
      : topicConsumerForm.watch("aclPatternType");
  const { data: topicTeam } = useQuery<TopicTeam, Error>({
    queryKey: ["topicTeam", topicName, selectedPatternType],
    queryFn: () =>
      getTopicTeam({ topicName, patternType: selectedPatternType }),
    keepPreviousData: true,
    onSuccess: (data) => {
      if (data === undefined) {
        throw new Error("Could not fetch team for current Topic");
      }
      return topicType === "Producer"
        ? topicProducerForm.setValue("teamname", data.team)
        : topicConsumerForm.setValue("teamname", data.team);
    },
  });

  const selectedEnvironment =
    topicType === "Producer"
      ? topicProducerForm.watch("environment")
      : topicConsumerForm.watch("environment");
  const selectedEnvironmentType =
    environments?.find((env) => env.id === selectedEnvironment)?.type || "";
  const { data: clusterInfo } = useQuery<ClusterInfo, Error>({
    queryKey: ["cluster-info", selectedEnvironment],
    queryFn: () =>
      getClusterInfo({
        envSelected: selectedEnvironment,
        envType: selectedEnvironmentType,
      }),

    keepPreviousData: false,
    enabled:
      selectedEnvironment !== "placeholder" && environments !== undefined,
    onSuccess: (data) => {
      const isAivenCluster = data?.aivenCluster === "true";
      // Enable the only possible option when the environment chosen is Aiven Kafka flavor
      if (isAivenCluster) {
        return topicType === "Producer"
          ? topicProducerForm.setValue("aclIpPrincipleType", "PRINCIPAL")
          : topicConsumerForm.setValue("aclIpPrincipleType", "PRINCIPAL");
      }
    },
  });

  if (
    topicNames === undefined ||
    environments === undefined ||
    topicTeam === undefined
  ) {
    return <SkeletonForm />;
  }

  return (
    <Box maxWidth={"4xl"}>
      {topicType === "Consumer" ? (
        <TopicConsumerForm
          renderAclTypeField={() => (
            <AclTypeField topicType={topicType} handleChange={setTopicType} />
          )}
          topicConsumerForm={topicConsumerForm}
          topicNames={topicNames}
          environments={environments}
          clusterInfo={clusterInfo}
        />
      ) : (
        <TopicProducerForm
          renderAclTypeField={() => (
            <AclTypeField topicType={topicType} handleChange={setTopicType} />
          )}
          topicProducerForm={topicProducerForm}
          topicNames={topicNames}
          environments={environments}
          clusterInfo={clusterInfo}
        />
      )}
    </Box>
  );
};

export default TopicAclRequest;
