import {
  Alert,
  Box,
  Divider,
  Grid,
  GridItem,
  RadioButton as BaseRadioButton,
  SecondaryButton,
} from "@aivenio/aquarium";
import { useMutation } from "@tanstack/react-query";
import { useEffect, useRef } from "react";
import { UseFormReturn } from "react-hook-form";
import { useNavigate } from "react-router-dom";
import {
  Form,
  RadioButtonGroup,
  SubmitButton,
  SubmitHandler,
  TextInput,
} from "src/app/components/Form";
import AclIpPrincipleTypeField from "src/app/features/topics/acl-request/fields/AclIpPrincipleTypeField";
import EnvironmentField from "src/app/features/topics/acl-request/fields/EnvironmentField";
import IpOrPrincipalField from "src/app/features/topics/acl-request/fields/IpOrPrincipalField";
import RemarksField from "src/app/features/topics/acl-request/fields/RemarksField";
import TopicNameOrPrefixField from "src/app/features/topics/acl-request/fields/TopicNameOrPrefixField";
import { TopicProducerFormSchema } from "src/app/features/topics/acl-request/schemas/topic-acl-request-producer";
import { createAclRequest } from "src/domain/acl/acl-api";
import { Environment } from "src/domain/environment";
import { parseErrorMsg } from "src/services/mutation-utils";

// eslint-disable-next-line import/exports-last
export interface TopicProducerFormProps {
  topicProducerForm: UseFormReturn<TopicProducerFormSchema>;
  topicNames: string[];
  environments: Environment[];
  renderAclTypeField: () => JSX.Element;
  isAivenCluster?: boolean;
}

const TopicProducerForm = ({
  topicProducerForm,
  topicNames,
  environments,
  renderAclTypeField,
  isAivenCluster,
}: TopicProducerFormProps) => {
  const navigate = useNavigate();
  const { aclIpPrincipleType, aclPatternType, topicname } =
    topicProducerForm.getValues();
  const { current: initialAclIpPrincipleType } = useRef(aclIpPrincipleType);
  const { current: initialAclPatternType } = useRef(aclPatternType);

  // Reset values of acl_ip and acl_ssl when user switches between IP or Principal
  // Not doing so results in values persisting to the form values if a value is entered and the field is then switched
  // Which causes errors
  useEffect(() => {
    // Prevents resetting when switching from Producer to Consumer forms
    if (aclIpPrincipleType === initialAclIpPrincipleType) {
      return;
    }

    topicProducerForm.resetField("acl_ip");
    topicProducerForm.resetField("acl_ssl");
  }, [aclIpPrincipleType]);

  // Reset values of topicname when user switches between LITERAL and PREFIXED
  // Avoids conflict when entering a prefix that is not an existing topic name
  useEffect(() => {
    // Prevents resetting when switching from Producer to Consumer forms
    if (
      aclPatternType === initialAclPatternType ||
      topicNames.includes(topicname)
    ) {
      return;
    }
    return topicProducerForm.setValue("topicname", topicNames[0]);
  }, [aclPatternType]);

  const { mutate, isLoading, isError, error } = useMutation({
    mutationFn: createAclRequest,
    onSuccess: () =>
      window.location.assign("/myAclRequests?reqsType=created&aclCreated=true"),
  });

  const onSubmitTopicProducer: SubmitHandler<TopicProducerFormSchema> = (
    formData
  ) => {
    mutate(formData);
  };

  const hideIpOrPrincipalField =
    aclIpPrincipleType === undefined || isAivenCluster === undefined;
  const hideTopicNameOrPrefixField = aclPatternType === undefined;

  return (
    <>
      {isError && (
        <Box marginBottom={"l1"} role="alert">
          <Alert type="warning">{parseErrorMsg(error)}</Alert>
        </Box>
      )}
      <Form {...topicProducerForm} onSubmit={onSubmitTopicProducer}>
        <Grid cols="2" minWidth={"fit"} colGap={"9"}>
          <GridItem>{renderAclTypeField()}</GridItem>
          <GridItem>
            <EnvironmentField environments={environments} />
          </GridItem>

          <GridItem colSpan={"span-2"} paddingBottom={"l2"}>
            <Divider />
          </GridItem>

          <GridItem>
            <RadioButtonGroup
              name="aclPatternType"
              labelText="Topic pattern type"
              disabled={topicNames.length === 0 || isAivenCluster}
              required
            >
              <BaseRadioButton value="LITERAL">Literal</BaseRadioButton>
              <BaseRadioButton value="PREFIXED">Prefixed</BaseRadioButton>
            </RadioButtonGroup>
          </GridItem>
          <GridItem>
            {hideTopicNameOrPrefixField ? (
              <Box data-testid="empty" style={{ height: "87px" }} />
            ) : (
              <TopicNameOrPrefixField
                topicNames={topicNames}
                aclPatternType={aclPatternType}
              />
            )}
          </GridItem>

          <GridItem colSpan={"span-2"}>
            {!isAivenCluster && (
              <TextInput
                name="transactionalId"
                labelText="Transactional ID"
                placeholder="Necessary for exactly-once semantics on producer"
              />
            )}
          </GridItem>

          <GridItem>
            <AclIpPrincipleTypeField isAivenCluster={isAivenCluster} />
          </GridItem>
          <GridItem>
            {hideIpOrPrincipalField ? (
              <Box data-testid={"empty"} style={{ height: "87px" }} />
            ) : (
              <IpOrPrincipalField
                aclIpPrincipleType={aclIpPrincipleType}
                isAivenCluster={isAivenCluster}
              />
            )}
          </GridItem>

          <GridItem colSpan={"span-2"} minWidth={"full"}>
            <RemarksField />
          </GridItem>
        </Grid>

        <Grid cols={"2"} colGap={"4"} width={"fit"}>
          <GridItem>
            <SubmitButton loading={isLoading}>Submit</SubmitButton>
          </GridItem>
          <GridItem>
            <SecondaryButton disabled={isLoading} onClick={() => navigate(-1)}>
              Cancel
            </SecondaryButton>
          </GridItem>
        </Grid>
      </Form>
    </>
  );
};

export default TopicProducerForm;
