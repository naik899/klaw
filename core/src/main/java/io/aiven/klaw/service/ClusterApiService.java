package io.aiven.klaw.service;

import static io.aiven.klaw.helpers.KwConstants.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.aiven.klaw.config.ManageDatabase;
import io.aiven.klaw.dao.AclRequests;
import io.aiven.klaw.dao.Env;
import io.aiven.klaw.dao.KwClusters;
import io.aiven.klaw.dao.SchemaRequest;
import io.aiven.klaw.error.KlawException;
import io.aiven.klaw.model.ApiResponse;
import io.aiven.klaw.model.KafkaSupportedProtocol;
import io.aiven.klaw.model.cluster.ClusterAclRequest;
import io.aiven.klaw.model.cluster.ClusterConnectorRequest;
import io.aiven.klaw.model.cluster.ClusterSchemaRequest;
import io.aiven.klaw.model.cluster.ClusterTopicRequest;
import io.aiven.klaw.model.enums.AclPatternType;
import io.aiven.klaw.model.enums.AclType;
import io.aiven.klaw.model.enums.AclsNativeType;
import io.aiven.klaw.model.enums.ApiResultStatus;
import io.aiven.klaw.model.enums.ClusterStatus;
import io.aiven.klaw.model.enums.KafkaClustersType;
import io.aiven.klaw.model.enums.KafkaFlavors;
import io.aiven.klaw.model.enums.RequestOperationType;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class ClusterApiService {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final String URL_DELIMITER = "/";

  @Autowired private ManageDatabase manageDatabase;

  @Value("${server.ssl.key-store:null}")
  private String keyStore;

  @Value("${server.ssl.key-store-password:null}")
  private String keyStorePwd;

  @Value("${server.ssl.key-store-type:JKS}")
  private String keyStoreType;

  @Value("${klaw.clusterapi.access.username}")
  private String clusterApiUser;

  @Value("${klaw.clusterapi.access.base64.secret:#{''}}")
  private String clusterApiAccessBase64Secret;

  private static String clusterConnUrl;
  protected static HttpComponentsClientHttpRequestFactory requestFactory;
  RestTemplate httpRestTemplate, httpsRestTemplate;

  public ClusterApiService(ManageDatabase manageDatabase) {
    this.manageDatabase = manageDatabase;
  }

  private RestTemplate getRestTemplate() {
    if (clusterConnUrl.toLowerCase().startsWith("https")) {
      if (this.httpsRestTemplate == null) {
        this.httpsRestTemplate = new RestTemplate(requestFactory);
      }
      return this.httpsRestTemplate;
    } else {
      if (this.httpRestTemplate == null) {
        this.httpRestTemplate = new RestTemplate();
      }
      return this.httpRestTemplate;
    }
  }

  private void getClusterApiProperties(int tenantId) {
    clusterConnUrl = manageDatabase.getKwPropertyValue(CLUSTER_CONN_URL_KEY, tenantId);
    if (clusterApiAccessBase64Secret.isBlank()) {
      log.info(
          "CONFIGURE CLUSTER API SECRET FOR CLUSTER OPERATIONS. klaw.clusterapi.access.base64.secret");
    }
  }

  public String getClusterApiStatus(String clusterApiUrl, boolean testConnection, int tenantId) {
    log.info(
        "getClusterApiStatus clusterApiUrl {} testConnection{}", clusterApiUrl, testConnection);
    getClusterApiProperties(tenantId);
    try {
      String uriClusterApiStatus = URI_CLUSTER_API;
      String uri;
      if (testConnection) {
        uri = clusterApiUrl + uriClusterApiStatus;
      } else {
        uri = clusterConnUrl + uriClusterApiStatus; // from stored kw props
      }

      ResponseEntity<ClusterStatus> resultBody =
          getRestTemplate().exchange(uri, HttpMethod.GET, getHttpEntity(), ClusterStatus.class);
      return Objects.requireNonNull(resultBody.getBody()).value;
    } catch (Exception e) {
      log.error("Error from getClusterApiStatus ", e);
      return ClusterStatus.OFFLINE.value;
    }
  }

  String getKafkaClusterStatus(
      String bootstrapHost,
      KafkaSupportedProtocol protocol,
      String clusterIdentification,
      String clusterType,
      String kafkaFlavor,
      int tenantId) {
    log.debug("getKafkaClusterStatus {} {}", bootstrapHost, protocol);
    getClusterApiProperties(tenantId);

    try {
      String uri =
          clusterConnUrl
              + URI_KAFKA_SR_CONN_STATUS
              + bootstrapHost
              + URL_DELIMITER
              + String.join(
                  URL_DELIMITER,
                  protocol.getName(),
                  clusterIdentification,
                  clusterType,
                  "kafkaFlavor",
                  kafkaFlavor);

      ResponseEntity<ClusterStatus> resultBody =
          getRestTemplate().exchange(uri, HttpMethod.GET, getHttpEntity(), ClusterStatus.class);
      return Objects.requireNonNull(resultBody.getBody()).value;
    } catch (Exception e) {
      log.error("Error from getKafkaClusterStatus ", e);
      return ClusterStatus.NOT_KNOWN.value;
    }
  }

  public List<Map<String, String>> getConsumerOffsets(
      String bootstrapHost,
      KafkaSupportedProtocol protocol,
      String clusterIdentification,
      String topic,
      String consumerGroupId,
      int tenantId)
      throws KlawException {
    log.info("getConsumerOffsets {} {} {} {}", bootstrapHost, protocol, topic, consumerGroupId);
    getClusterApiProperties(tenantId);
    List<Map<String, String>> offsetsMap;
    try {
      String url = URI_GET_CONSUMER_OFFSETS;
      url =
          clusterConnUrl
              + url
              + bootstrapHost
              + URL_DELIMITER
              + String.join(
                  URL_DELIMITER, protocol.getName(), clusterIdentification, consumerGroupId, topic);

      ResponseEntity<List<Map<String, String>>> resultBody =
          getRestTemplate()
              .exchange(
                  url, HttpMethod.GET, getHttpEntity(), new ParameterizedTypeReference<>() {});

      offsetsMap = new ArrayList<>(Objects.requireNonNull(resultBody.getBody()));
    } catch (Exception e) {
      log.error("Error from getConsumerOffsets ", e);
      throw new KlawException("Could not get consumer offsets");
    }
    return offsetsMap;
  }

  public Map<String, String> getTopicEvents(
      String bootstrapHost,
      KafkaSupportedProtocol protocol,
      String clusterIdentification,
      String topic,
      String offsetId,
      String consumerGroupId,
      int tenantId)
      throws KlawException {
    log.info(
        "getTopicEvents {} {} {} {} {}", bootstrapHost, protocol, topic, offsetId, consumerGroupId);
    getClusterApiProperties(tenantId);
    Map<String, String> eventsMap;
    try {
      String url = URI_GET_TOPIC_CONTENTS;
      url =
          clusterConnUrl
              + url
              + bootstrapHost
              + URL_DELIMITER
              + String.join(
                  URL_DELIMITER,
                  protocol.getName(),
                  clusterIdentification,
                  consumerGroupId,
                  topic,
                  offsetId);

      ResponseEntity<Map<String, String>> resultBody =
          getRestTemplate()
              .exchange(
                  url, HttpMethod.GET, getHttpEntity(), new ParameterizedTypeReference<>() {});

      eventsMap = new TreeMap<>(Objects.requireNonNull(resultBody.getBody()));
    } catch (Exception e) {
      log.error("Error from getTopicEvents {} ", topic, e);
      throw new KlawException("Could not get events for Topic " + topic);
    }
    return eventsMap;
  }

  public List<Map<String, String>> getAcls(
      String bootstrapHost, Env envSelected, KafkaSupportedProtocol protocol, int tenantId)
      throws KlawException {
    log.info("getAcls {} {} {}", bootstrapHost, protocol, tenantId);
    getClusterApiProperties(tenantId);

    List<Map<String, String>> aclListOriginal;
    try {
      String uriGetAcls = URI_GET_ACLS;
      KwClusters kwClusters =
          manageDatabase
              .getClusters(KafkaClustersType.KAFKA, tenantId)
              .get(envSelected.getClusterId());

      String uri;
      // aiven config
      if (KafkaFlavors.AIVEN_FOR_APACHE_KAFKA.value.equals(kwClusters.getKafkaFlavor())) {
        uri =
            clusterConnUrl
                + uriGetAcls
                + bootstrapHost
                + URL_DELIMITER
                + String.join(
                    URL_DELIMITER,
                    AclsNativeType.AIVEN.name(),
                    protocol.getName(),
                    kwClusters.getClusterName() + kwClusters.getClusterId(),
                    kwClusters.getProjectName(),
                    kwClusters.getServiceName());
      } // confluent cloud config
      else if (KafkaFlavors.CONFLUENT_CLOUD.value.equals(kwClusters.getKafkaFlavor())) {
        uri =
            clusterConnUrl
                + uriGetAcls
                + bootstrapHost
                + URL_DELIMITER
                + String.join(
                    URL_DELIMITER,
                    AclsNativeType.CONFLUENT_CLOUD.name(),
                    protocol.getName(),
                    kwClusters.getClusterName() + kwClusters.getClusterId(),
                    kwClusters.getProjectName(),
                    kwClusters.getServiceName());
      } else {
        uri =
            clusterConnUrl
                + uriGetAcls
                + bootstrapHost
                + URL_DELIMITER
                + String.join(
                    URL_DELIMITER,
                    AclsNativeType.NATIVE.name(),
                    protocol.getName(),
                    kwClusters.getClusterName() + kwClusters.getClusterId(),
                    "na",
                    "na");
      }

      ResponseEntity<Set<Map<String, String>>> resultBody =
          getRestTemplate()
              .exchange(
                  uri, HttpMethod.GET, getHttpEntity(), new ParameterizedTypeReference<>() {});
      aclListOriginal = new ArrayList<>(Objects.requireNonNull(resultBody.getBody()));
    } catch (Exception e) {
      log.error("Error from getAcls", e);
      throw new KlawException("Could not load topics/acls. Please contact Administrator.");
    }
    return aclListOriginal;
  }

  public List<Map<String, String>> getAllTopics(
      String bootstrapHost,
      KafkaSupportedProtocol protocol,
      String clusterIdentification,
      String kafkaFlavors,
      int tenantId)
      throws Exception {
    log.info("getAllTopics {} {}", bootstrapHost, protocol);
    getClusterApiProperties(tenantId);
    List<Map<String, String>> topicsList;
    String aclsNativeType = AclsNativeType.NATIVE.value;
    if (KafkaFlavors.CONFLUENT_CLOUD.value.equals(kafkaFlavors)) {
      aclsNativeType = AclsNativeType.CONFLUENT_CLOUD.value;
    }
    try {
      String uriGetTopicsFull =
          clusterConnUrl
              + URI_GET_TOPICS
              + bootstrapHost
              + URL_DELIMITER
              + String.join(
                  URL_DELIMITER,
                  protocol.getName(),
                  clusterIdentification,
                  "topicsNativeType",
                  aclsNativeType);

      HttpEntity<String> entity = getHttpEntity();
      ResponseEntity<Set<Map<String, String>>> s =
          getRestTemplate()
              .exchange(
                  uriGetTopicsFull, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});
      topicsList = new ArrayList<>(Objects.requireNonNull(s.getBody()));
    } catch (Exception e) {
      log.error("Error from getAllTopics", e);
      throw new KlawException("Could not load topics. Please contact Administrator.");
    }

    return topicsList;
  }

  public String approveConnectorRequests(
      String connectorName,
      KafkaSupportedProtocol protocol,
      String connectorType,
      String connectorConfig,
      String kafkaConnectHost,
      String clusterIdentification,
      int tenantId)
      throws KlawException {
    log.info("approveConnectorRequests {} {}", connectorConfig, kafkaConnectHost);
    getClusterApiProperties(tenantId);
    ResponseEntity<Map<String, String>> response;
    try {
      ClusterConnectorRequest clusterConnectorRequest =
          ClusterConnectorRequest.builder()
              .env(kafkaConnectHost)
              .connectorName(connectorName)
              .connectorConfig(connectorConfig)
              .protocol(protocol)
              .clusterIdentification(clusterIdentification)
              .build();

      String uri;

      if (RequestOperationType.CREATE.value.equals(connectorType)) {
        uri = clusterConnUrl + URI_POST_CONNECTOR;
      } else if (RequestOperationType.UPDATE.value.equals(connectorType)) {
        uri = clusterConnUrl + URI_UPDATE_CONNECTOR;
      } else {
        uri = clusterConnUrl + URI_DELETE_CONNECTOR;
      }

      HttpHeaders headers = createHeaders(clusterApiUser);
      headers.setContentType(MediaType.APPLICATION_JSON);

      HttpEntity<ClusterConnectorRequest> request =
          new HttpEntity<>(clusterConnectorRequest, headers);
      response =
          getRestTemplate()
              .exchange(uri, HttpMethod.POST, request, new ParameterizedTypeReference<>() {});

      if (ApiResultStatus.SUCCESS.value.equals(
          Objects.requireNonNull(response.getBody()).get("result"))) {
        return ApiResultStatus.SUCCESS.value;
      } else {
        return response.getBody().get("errorText");
      }

    } catch (Exception e) {
      log.error("approveConnectorRequests {} ", connectorName, e);
      throw new KlawException("Could not approve connector request. Please contact Administrator.");
    }
  }

  public ResponseEntity<ApiResponse> approveTopicRequests(
      String topicName,
      String topicRequestType,
      int topicPartitions,
      String replicationFactor,
      String topicEnvId,
      Map<String, String> advancedTopicConfiguration,
      int tenantId)
      throws KlawException {
    log.info("approveTopicRequests {} {}", topicName, topicEnvId);
    getClusterApiProperties(tenantId);
    ResponseEntity<ApiResponse> response;
    ClusterTopicRequest clusterTopicRequest;
    try {
      Env envSelected = manageDatabase.getHandleDbRequests().selectEnvDetails(topicEnvId, tenantId);
      KwClusters kwClusters =
          manageDatabase
              .getClusters(KafkaClustersType.KAFKA, tenantId)
              .get(envSelected.getClusterId());
      clusterTopicRequest =
          ClusterTopicRequest.builder()
              .env(kwClusters.getBootstrapServers())
              .protocol(kwClusters.getProtocol())
              .clusterName(kwClusters.getClusterName() + kwClusters.getClusterId())
              .topicName(topicName)
              .aclsNativeType(
                  (Objects.equals(kwClusters.getKafkaFlavor(), KafkaFlavors.CONFLUENT_CLOUD.value))
                      ? AclsNativeType.CONFLUENT_CLOUD
                      : AclsNativeType.NATIVE)
              .build();

      String uri;
      if (RequestOperationType.CREATE.value.equals(topicRequestType)
          || RequestOperationType.PROMOTE.value.equals(topicRequestType)) {
        uri = clusterConnUrl + URI_CREATE_TOPICS;
        clusterTopicRequest =
            clusterTopicRequest.toBuilder()
                .partitions(topicPartitions)
                .replicationFactor(Short.parseShort(replicationFactor))
                .advancedTopicConfiguration(advancedTopicConfiguration)
                .build();
      } else if (RequestOperationType.UPDATE.value.equals(topicRequestType)) {
        uri = clusterConnUrl + URI_UPDATE_TOPICS;
        clusterTopicRequest =
            clusterTopicRequest.toBuilder()
                .partitions(topicPartitions)
                .replicationFactor(Short.parseShort(replicationFactor))
                .build();
      } else {
        uri = clusterConnUrl + URI_DELETE_TOPICS;
      }

      HttpHeaders headers = createHeaders(clusterApiUser);
      headers.setContentType(MediaType.APPLICATION_JSON);
      HttpEntity<ClusterTopicRequest> request = new HttpEntity<>(clusterTopicRequest, headers);
      response = getRestTemplate().postForEntity(uri, request, ApiResponse.class);
    } catch (Exception e) {
      log.error("approveTopicRequests {}", topicName, e);
      throw new KlawException("Could not approve topic request. Please contact Administrator.");
    }
    return response;
  }

  public ResponseEntity<ApiResponse> approveAclRequests(AclRequests aclReq, int tenantId)
      throws KlawException {
    log.info("approveAclRequests {}", aclReq);
    getClusterApiProperties(tenantId);
    ResponseEntity<ApiResponse> response;

    try {
      String env = aclReq.getEnvironment();
      String uri;

      ClusterAclRequest clusterAclRequest;
      Env envSelected = manageDatabase.getHandleDbRequests().selectEnvDetails(env, tenantId);
      KwClusters kwClusters =
          manageDatabase
              .getClusters(KafkaClustersType.KAFKA, tenantId)
              .get(envSelected.getClusterId());

      // aiven config
      if (Objects.equals(KafkaFlavors.AIVEN_FOR_APACHE_KAFKA.value, kwClusters.getKafkaFlavor())) {
        clusterAclRequest =
            ClusterAclRequest.builder()
                .aclNativeType(AclsNativeType.AIVEN.name())
                .projectName(kwClusters.getProjectName())
                .serviceName(kwClusters.getServiceName())
                .topicName(aclReq.getTopicname())
                .username(aclReq.getAcl_ssl())
                .build();

        if (Objects.equals(aclReq.getTopictype(), AclType.PRODUCER.value)) {
          clusterAclRequest = clusterAclRequest.toBuilder().permission("write").build();
        } else {
          clusterAclRequest = clusterAclRequest.toBuilder().permission("read").build();
        }

        if (Objects.equals(RequestOperationType.DELETE.value, aclReq.getAclType())
            && null != aclReq.getJsonParams()) {
          Map<String, String> jsonObj =
              OBJECT_MAPPER.readValue(aclReq.getJsonParams(), new TypeReference<>() {});
          String aivenAclKey = "aivenaclid";
          if (jsonObj.containsKey(aivenAclKey)) {
            clusterAclRequest =
                clusterAclRequest.toBuilder().aivenAclKey(jsonObj.get(aivenAclKey)).build();
          } else {
            log.error("Error from approveAclRequests : AclId - aivenaclid not found");
            throw new KlawException(
                "Could not approve acl request. AclId - Aiven acl id not found.");
          }
        }
      } else if (Objects.equals(KafkaFlavors.CONFLUENT_CLOUD.value, kwClusters.getKafkaFlavor())) {
        String aclPatternType = aclReq.getAclPatternType();
        clusterAclRequest =
            ClusterAclRequest.builder()
                .aclNativeType(AclsNativeType.CONFLUENT_CLOUD.name())
                .env(kwClusters.getBootstrapServers())
                .protocol(kwClusters.getProtocol())
                .clusterName(kwClusters.getClusterName() + kwClusters.getClusterId())
                .topicName(aclReq.getTopicname())
                .consumerGroup(aclReq.getConsumergroup())
                .aclType(aclReq.getTopictype())
                .aclIp(aclReq.getAcl_ip())
                .aclSsl(aclReq.getAcl_ssl())
                .transactionalId(aclReq.getTransactionalId())
                .aclIpPrincipleType(aclReq.getAclIpPrincipleType().name())
                .isPrefixAcl(AclPatternType.PREFIXED.value.equals(aclPatternType))
                .build();
      } else {
        String aclPatternType = aclReq.getAclPatternType();
        clusterAclRequest =
            ClusterAclRequest.builder()
                .aclNativeType(AclsNativeType.NATIVE.name())
                .env(kwClusters.getBootstrapServers())
                .protocol(kwClusters.getProtocol())
                .clusterName(kwClusters.getClusterName() + kwClusters.getClusterId())
                .topicName(aclReq.getTopicname())
                .consumerGroup(aclReq.getConsumergroup())
                .aclType(aclReq.getTopictype())
                .aclIp(aclReq.getAcl_ip())
                .aclSsl(aclReq.getAcl_ssl())
                .transactionalId(aclReq.getTransactionalId())
                .aclIpPrincipleType(aclReq.getAclIpPrincipleType().name())
                .isPrefixAcl(AclPatternType.PREFIXED.value.equals(aclPatternType))
                .build();
      }

      if (RequestOperationType.CREATE.value.equals(aclReq.getAclType())) {
        uri = clusterConnUrl + URI_CREATE_ACLS;
        clusterAclRequest =
            clusterAclRequest.toBuilder().requestOperationType(RequestOperationType.CREATE).build();
      } else {
        uri = clusterConnUrl + URI_DELETE_ACLS;
        clusterAclRequest =
            clusterAclRequest.toBuilder().requestOperationType(RequestOperationType.DELETE).build();
      }

      HttpHeaders headers = createHeaders(clusterApiUser);
      headers.setContentType(MediaType.APPLICATION_JSON);

      HttpEntity<ClusterAclRequest> request = new HttpEntity<>(clusterAclRequest, headers);
      response =
          getRestTemplate()
              .exchange(uri, HttpMethod.POST, request, new ParameterizedTypeReference<>() {});
      return response;
    } catch (Exception e) {
      log.error("Error from approveAclRequests", e);
      throw new KlawException("Could not approve acl request. Please contact Administrator.");
    }
  }

  public ApiResponse getAivenServiceAccountDetails(
      String projectName, String serviceName, String userName, int tenantId) throws KlawException {
    getClusterApiProperties(tenantId);
    try {
      String uriGetServiceAccountDetails = clusterConnUrl + URI_AIVEN_SERVICE_ACCOUNT_DETAIL;
      uriGetServiceAccountDetails =
          uriGetServiceAccountDetails
              .replace("projectName", projectName)
              .replace("serviceName", serviceName)
              .replace("userName", userName);

      HttpEntity<String> entity = getHttpEntity();
      ResponseEntity<ApiResponse> apiResponseResponseEntity =
          getRestTemplate()
              .exchange(
                  uriGetServiceAccountDetails,
                  HttpMethod.GET,
                  entity,
                  new ParameterizedTypeReference<>() {});
      return apiResponseResponseEntity.getBody();
    } catch (Exception e) {
      log.error("Error from getAivenServiceAccountDetails", e);
      throw new KlawException(
          "Could not retrieve service account details. Please contact Administrator.");
    }
  }

  public ApiResponse getAivenServiceAccounts(String projectName, String serviceName, int tenantId)
      throws KlawException {
    getClusterApiProperties(tenantId);
    try {
      String uriGetServiceAccounts = clusterConnUrl + URI_AIVEN_SERVICE_ACCOUNTS;
      uriGetServiceAccounts =
          uriGetServiceAccounts
              .replace("projectName", projectName)
              .replace("serviceName", serviceName);

      HttpEntity<String> entity = getHttpEntity();
      ResponseEntity<ApiResponse> apiResponseResponseEntity =
          getRestTemplate()
              .exchange(
                  uriGetServiceAccounts,
                  HttpMethod.GET,
                  entity,
                  new ParameterizedTypeReference<>() {});
      return apiResponseResponseEntity.getBody();
    } catch (Exception e) {
      log.error("Error from getAivenServiceAccounts", e);
      throw new KlawException("Could not retrieve service accounts. Please contact Administrator.");
    }
  }

  ResponseEntity<ApiResponse> postSchema(
      SchemaRequest schemaRequest, String env, String topicName, int tenantId)
      throws KlawException {
    log.info("postSchema {} {}", topicName, env);
    getClusterApiProperties(tenantId);
    ResponseEntity<ApiResponse> response;
    try {
      String uri = clusterConnUrl + URI_POST_SCHEMA;

      Env envSelected = manageDatabase.getHandleDbRequests().selectEnvDetails(env, tenantId);
      KwClusters kwClusters =
          manageDatabase
              .getClusters(KafkaClustersType.SCHEMA_REGISTRY, tenantId)
              .get(envSelected.getClusterId());
      ClusterSchemaRequest clusterSchemaRequest =
          ClusterSchemaRequest.builder()
              .protocol(kwClusters.getProtocol())
              .env(kwClusters.getBootstrapServers())
              .topicName(topicName)
              .fullSchema(schemaRequest.getSchemafull())
              .clusterIdentification(kwClusters.getClusterName() + kwClusters.getClusterId())
              .forceRegister(schemaRequest.isForceRegister())
              .build();

      HttpHeaders headers = createHeaders(clusterApiUser);
      headers.setContentType(MediaType.APPLICATION_JSON);

      HttpEntity<ClusterSchemaRequest> request = new HttpEntity<>(clusterSchemaRequest, headers);
      response = getRestTemplate().postForEntity(uri, request, ApiResponse.class);
    } catch (Exception e) {
      log.error("Error from postSchema ", e);
      throw new KlawException("Could not post schema. Please contact Administrator.");
    }
    return response;
  }

  public TreeMap<Integer, Map<String, Object>> getAvroSchema(
      String schemaRegistryHost,
      KafkaSupportedProtocol protocol,
      String clusterIdentification,
      String topicName,
      int tenantId)
      throws Exception {
    log.info("getAvroSchema {} {}", schemaRegistryHost, topicName);
    getClusterApiProperties(tenantId);
    TreeMap<Integer, Map<String, Object>> allVersionSchemas =
        new TreeMap<>(Collections.reverseOrder());
    try {
      String uriGetTopicsFull =
          clusterConnUrl
              + URI_GET_SCHEMA
              + schemaRegistryHost
              + URL_DELIMITER
              + String.join(URL_DELIMITER, protocol.getName(), clusterIdentification, topicName);

      ResponseEntity<TreeMap<String, Map<String, Object>>> treeMapResponseEntity =
          getRestTemplate()
              .exchange(
                  uriGetTopicsFull,
                  HttpMethod.GET,
                  getHttpEntity(),
                  new ParameterizedTypeReference<>() {});

      for (String schemaVersion :
          Objects.requireNonNull(treeMapResponseEntity.getBody()).keySet()) {
        allVersionSchemas.put(
            Integer.parseInt(schemaVersion), treeMapResponseEntity.getBody().get(schemaVersion));
      }

      return allVersionSchemas;
    } catch (Exception e) {
      log.error("Error from getAvroSchema ", e);
      throw new KlawException("Could not get schema.");
    }
  }

  public Map<String, Object> getConnectorDetails(
      String connectorName,
      String kafkaConnectHost,
      KafkaSupportedProtocol protocol,
      String clusterIdentification,
      int tenantId)
      throws KlawException {
    log.info("getConnectorDetails {} {}", connectorName, kafkaConnectHost);
    getClusterApiProperties(tenantId);
    try {
      String uriGetTopics =
          String.join(
              URL_DELIMITER,
              URI_CONNECTOR_DETAILS,
              connectorName,
              kafkaConnectHost,
              protocol.getName(),
              clusterIdentification);
      String uriGetConnectorsFull = clusterConnUrl + uriGetTopics;

      ResponseEntity<Map<String, Object>> s =
          getRestTemplate()
              .exchange(
                  uriGetConnectorsFull,
                  HttpMethod.GET,
                  getHttpEntity(),
                  new ParameterizedTypeReference<>() {});

      return s.getBody();
    } catch (Exception e) {
      log.error("Error from getConnectorDetails ", e);
      throw new KlawException("Could not get Connector Details." + connectorName);
    }
  }

  public ArrayList<String> getAllKafkaConnectors(
      String kafkaConnectHost, String protocol, String clusterIdentification, int tenantId)
      throws KlawException {
    log.info("getAllKafkaConnectors {}", kafkaConnectHost);
    getClusterApiProperties(tenantId);
    try {
      String uriGetTopics =
          URI_GET_ALL_CONNECTORS + kafkaConnectHost + "/" + protocol + "/" + clusterIdentification;
      String uriGetConnectorsFull = clusterConnUrl + uriGetTopics;

      ResponseEntity<ArrayList<String>> s =
          getRestTemplate()
              .exchange(
                  uriGetConnectorsFull,
                  HttpMethod.GET,
                  getHttpEntity(),
                  new ParameterizedTypeReference<>() {});

      return s.getBody();
    } catch (Exception e) {
      log.error("Error from getAllKafkaConnectors ", e);
      throw new KlawException("Could not get KafkaConnectors.");
    }
  }

  public Map<String, String> retrieveMetrics(String jmxUrl, String objectName)
      throws KlawException {
    log.info("retrieveMetrics {} {}", jmxUrl, objectName);
    getClusterApiProperties(101);
    try {
      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("jmxUrl", jmxUrl);
      params.add("objectName", objectName);

      String uriGetTopicsFull = clusterConnUrl + URI_GET_METRICS;
      RestTemplate restTemplate = getRestTemplate();

      HttpHeaders headers = createHeaders(clusterApiUser);
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
      HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

      ResponseEntity<HashMap<String, String>> s =
          restTemplate.exchange(
              uriGetTopicsFull, HttpMethod.POST, entity, new ParameterizedTypeReference<>() {});

      return Objects.requireNonNull(s.getBody());

    } catch (Exception e) {
      log.error("Error from  retrieveMetrics {} ", jmxUrl, e);
      throw new KlawException("Could not get metrics.");
    }
  }

  // to connect to cluster api if https
  @PostConstruct
  private void setKwSSLContext() {
    if (keyStore != null && !keyStore.equals("null")) {
      TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;
      SSLContextBuilder builder = new SSLContextBuilder();
      try {
        builder
            .loadKeyMaterial(getStore(keyStorePwd, keyStore), keyStorePwd.toCharArray())
            .loadTrustMaterial(acceptingTrustStrategy);
        SSLConnectionSocketFactory sslsf =
            new SSLConnectionSocketFactory(builder.build(), NoopHostnameVerifier.INSTANCE);
        Registry<ConnectionSocketFactory> registry =
            RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", new PlainConnectionSocketFactory())
                .register("https", sslsf)
                .build();
        HttpClientConnectionManager poolingConnManager =
            new PoolingHttpClientConnectionManager(registry);
        CloseableHttpClient httpClient =
            HttpClients.custom().setConnectionManager(poolingConnManager).build();
        requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient);
      } catch (NoSuchAlgorithmException
          | KeyStoreException
          | CertificateException
          | UnrecoverableKeyException
          | IOException
          | KeyManagementException e) {
        log.error("Exception: ", e);
        throw new RuntimeException(e);
      }
    }
  }

  protected KeyStore getStore(String secret, String storeLoc)
      throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {

    File key = ResourceUtils.getFile(storeLoc);
    final KeyStore store = KeyStore.getInstance(keyStoreType);
    try (InputStream inputStream = new FileInputStream(key)) {
      store.load(inputStream, secret.toCharArray());
    }
    return store;
  }

  private HttpHeaders createHeaders(String username) throws KlawException {
    HttpHeaders httpHeaders = new HttpHeaders();
    String authHeader = "Bearer " + generateToken(username);
    httpHeaders.set("Authorization", authHeader);

    return httpHeaders;
  }

  private String generateToken(String username) throws KlawException {
    if (clusterApiAccessBase64Secret.isBlank()) {
      String errorTxt =
          "CONFIGURE CLUSTER API SECRET FOR CLUSTER OPERATIONS. klaw.clusterapi.access.base64.secret";
      log.error(errorTxt);
      throw new KlawException(errorTxt);
    }

    Key hmacKey =
        new SecretKeySpec(
            Base64.decodeBase64(clusterApiAccessBase64Secret),
            SignatureAlgorithm.HS256.getJcaName());
    Instant now = Instant.now();

    return Jwts.builder()
        .claim("name", username)
        .setSubject(username)
        .setId(UUID.randomUUID().toString())
        .setIssuedAt(Date.from(now))
        .setExpiration(Date.from(now.plus(3L, ChronoUnit.MINUTES))) // expiry in 3 minutes
        .signWith(hmacKey)
        .compact();
  }

  private HttpEntity<String> getHttpEntity() throws KlawException {
    HttpHeaders headers = createHeaders(clusterApiUser);
    headers.setContentType(MediaType.APPLICATION_JSON);

    headers.add("Accept", MediaType.APPLICATION_JSON_VALUE);
    return new HttpEntity<>(headers);
  }
}
