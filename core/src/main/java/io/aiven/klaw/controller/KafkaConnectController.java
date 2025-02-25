package io.aiven.klaw.controller;

import io.aiven.klaw.error.KlawException;
import io.aiven.klaw.model.ApiResponse;
import io.aiven.klaw.model.ConnectorOverview;
import io.aiven.klaw.model.KafkaConnectorModel;
import io.aiven.klaw.model.KafkaConnectorRequestModel;
import io.aiven.klaw.service.KafkaConnectControllerService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
@Slf4j
public class KafkaConnectController {

  @Autowired KafkaConnectControllerService kafkaConnectControllerService;

  @PostMapping(value = "/createConnector")
  public ResponseEntity<ApiResponse> createConnectorRequest(
      @Valid @RequestBody KafkaConnectorRequestModel addTopicRequest) throws KlawException {
    return new ResponseEntity<>(
        kafkaConnectControllerService.createConnectorRequest(addTopicRequest), HttpStatus.OK);
  }

  /**
   * @param pageNo Which page would you like returned e.g. 1
   * @param currentPage Which Page are you currently on e.g. 1
   * @param requestsType What type of requests are you looking for e.g. 'created' or 'deleted'
   * @param env The name of the environment you would like returned e.g. '1' or '4'
   * @param search A wildcard search term that searches topicNames.
   * @return A List of Kafka Connector Requests filtered by the provided parameters.
   */
  @RequestMapping(
      value = "/getConnectorRequestsForApproval",
      method = RequestMethod.GET,
      produces = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<List<KafkaConnectorRequestModel>> getCreatedConnectorRequests(
      @RequestParam("pageNo") String pageNo,
      @RequestParam(value = "currentPage", defaultValue = "") String currentPage,
      @RequestParam(value = "requestsType", defaultValue = "created") String requestsType,
      @RequestParam(value = "env", required = false) String env,
      @RequestParam(value = "search", required = false) String search) {
    return new ResponseEntity<>(
        kafkaConnectControllerService.getCreatedConnectorRequests(
            pageNo, currentPage, requestsType, env, search),
        HttpStatus.OK);
  }

  @RequestMapping(
      value = "/deleteConnectorRequests",
      method = RequestMethod.POST,
      produces = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<ApiResponse> deleteConnectorRequests(
      @RequestParam("connectorId") String connectorId) throws KlawException {
    return new ResponseEntity<>(
        kafkaConnectControllerService.deleteConnectorRequests(connectorId), HttpStatus.OK);
  }

  @PostMapping(
      value = "/execConnectorRequests",
      produces = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<ApiResponse> approveTopicRequests(
      @RequestParam("connectorId") String connectorId) throws KlawException {
    return new ResponseEntity<>(
        kafkaConnectControllerService.approveConnectorRequests(connectorId), HttpStatus.OK);
  }

  @PostMapping(
      value = "/execConnectorRequestsDecline",
      produces = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<ApiResponse> declineConnectorRequests(
      @RequestParam("connectorId") String connectorId,
      @RequestParam("reasonForDecline") String reasonForDecline)
      throws KlawException {
    return new ResponseEntity<>(
        kafkaConnectControllerService.declineConnectorRequests(connectorId, reasonForDecline),
        HttpStatus.OK);
  }

  @PostMapping(
      value = "/createConnectorDeleteRequest",
      produces = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<ApiResponse> createConnectorDeleteRequest(
      @RequestParam("connectorName") String topicName, @RequestParam("env") String envId)
      throws KlawException {
    return new ResponseEntity<>(
        kafkaConnectControllerService.createConnectorDeleteRequest(topicName, envId),
        HttpStatus.OK);
  }

  @RequestMapping(
      value = "/getConnectorRequests",
      method = RequestMethod.GET,
      produces = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<List<KafkaConnectorRequestModel>> getConnectorRequests(
      @RequestParam("pageNo") String pageNo,
      @RequestParam(value = "currentPage", defaultValue = "") String currentPage,
      @RequestParam(value = "requestsType", defaultValue = "all") String requestsType) {
    return new ResponseEntity<>(
        kafkaConnectControllerService.getConnectorRequests(pageNo, currentPage, requestsType),
        HttpStatus.OK);
  }

  @RequestMapping(
      value = "/getConnectors",
      method = RequestMethod.GET,
      produces = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<List<List<KafkaConnectorModel>>> getTopics(
      @RequestParam("env") String envId,
      @RequestParam("pageNo") String pageNo,
      @RequestParam(value = "currentPage", defaultValue = "") String currentPage,
      @RequestParam(value = "connectornamesearch", required = false) String topicNameSearch,
      @RequestParam(value = "teamName", required = false) String teamName)
      throws Exception {
    return new ResponseEntity<>(
        kafkaConnectControllerService.getConnectors(
            envId, pageNo, currentPage, topicNameSearch, teamName),
        HttpStatus.OK);
  }

  @RequestMapping(
      value = "/getConnectorOverview",
      method = RequestMethod.GET,
      produces = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<ConnectorOverview> getConnectorOverview(
      @RequestParam(value = "connectornamesearch") String connectorNameSearch) {
    return new ResponseEntity<>(
        kafkaConnectControllerService.getConnectorOverview(connectorNameSearch), HttpStatus.OK);
  }

  @PostMapping(
      value = "/createClaimConnectorRequest",
      produces = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<ApiResponse> createClaimConnectorRequest(
      @RequestParam("connectorName") String connectorName, @RequestParam("env") String envId)
      throws KlawException {
    return new ResponseEntity<>(
        kafkaConnectControllerService.createClaimConnectorRequest(connectorName, envId),
        HttpStatus.OK);
  }

  @PostMapping(value = "/saveConnectorDocumentation")
  public ResponseEntity<ApiResponse> saveConnectorDocumentation(
      @RequestBody KafkaConnectorModel topicInfo) {
    return new ResponseEntity<>(
        kafkaConnectControllerService.saveConnectorDocumentation(topicInfo), HttpStatus.OK);
  }

  @RequestMapping(
      value = "/getConnectorDetailsPerEnv",
      method = RequestMethod.GET,
      produces = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<Map<String, Object>> getConnectorDetailsPerEnv(
      @RequestParam("envSelected") String envId,
      @RequestParam("connectorName") String connectorName) {
    return new ResponseEntity<>(
        kafkaConnectControllerService.getConnectorDetailsPerEnv(envId, connectorName),
        HttpStatus.OK);
  }
}
