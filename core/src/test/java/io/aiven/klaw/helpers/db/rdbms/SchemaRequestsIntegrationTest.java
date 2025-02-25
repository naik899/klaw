package io.aiven.klaw.helpers.db.rdbms;

import static org.assertj.core.api.Assertions.assertThat;

import io.aiven.klaw.UtilMethods;
import io.aiven.klaw.dao.SchemaRequest;
import io.aiven.klaw.dao.Team;
import io.aiven.klaw.dao.UserInfo;
import io.aiven.klaw.model.enums.RequestMode;
import io.aiven.klaw.model.enums.RequestOperationType;
import io.aiven.klaw.model.enums.RequestStatus;
import io.aiven.klaw.repository.SchemaRequestRepo;
import io.aiven.klaw.repository.UserInfoRepo;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SchemaRequestsIntegrationTest {

  @Autowired private SchemaRequestRepo schemaRequestRepo;
  @Autowired private UserInfoRepo userInfoRepo;

  @Autowired TestEntityManager entityManager;

  private SelectDataJdbc selectDataJdbc;

  private UtilMethods utilMethods;

  public void loadData() {
    generateData(
        5,
        101,
        101,
        "firsttopic1",
        "dev",
        RequestStatus.CREATED,
        RequestOperationType.CREATE,
        100,
        "Jackie"); // Team1
    generateData(
        3,
        101,
        101,
        "firsttopic2",
        "dev",
        RequestStatus.APPROVED,
        RequestOperationType.CREATE,
        200,
        "Jackie"); // Team1
    generateData(
        7,
        101,
        101,
        "firsttopic3",
        "dev",
        RequestStatus.DECLINED,
        RequestOperationType.CREATE,
        300,
        "Jackie"); // Team1
    generateData(
        2,
        101,
        101,
        "firsttopic4",
        "dev",
        RequestStatus.DELETED,
        RequestOperationType.DELETE,
        400,
        "Jackie"); // Team1
    generateData(
        4,
        103,
        101,
        "firsttopic5",
        "dev",
        RequestStatus.CREATED,
        RequestOperationType.DELETE,
        500,
        "James"); // Team2
  }

  public void setupUserInformation() {
    UserInfo user = new UserInfo();
    user.setTenantId(101);
    user.setTeamId(101);
    user.setRole("USER");
    user.setUsername("James");
    UserInfo user2 = new UserInfo();
    user2.setTenantId(103);
    user2.setTeamId(103);
    user2.setRole("USER");
    user2.setUsername("John");
    entityManager.persistAndFlush(user);
    entityManager.persistAndFlush(user2);
    UserInfo user3 = new UserInfo();
    user3.setTenantId(101);
    user3.setTeamId(101);
    user3.setRole("USER");
    user3.setUsername("Jackie");
    entityManager.persistAndFlush(user3);
    Team t1 = new Team();
    t1.setTeamId(101);
    t1.setTenantId(101);
    t1.setTeamname("octopus");
    Team t2 = new Team();
    t2.setTeamname("pirates");
    t2.setTeamId(103);
    t2.setTenantId(103);

    Team t4 = new Team();
    t4.setTeamname("plank");
    t4.setTeamId(104);
    t4.setTenantId(101);

    entityManager.persistAndFlush(t1);
    entityManager.persistAndFlush(t2);
    entityManager.persistAndFlush(t4);
  }

  @BeforeEach
  public void setUp() {
    selectDataJdbc = new SelectDataJdbc();
    utilMethods = new UtilMethods();
    ReflectionTestUtils.setField(selectDataJdbc, "schemaRequestRepo", schemaRequestRepo);
    ReflectionTestUtils.setField(selectDataJdbc, "userInfoRepo", userInfoRepo);
    loadData();
    setupUserInformation();
  }

  @Test
  @Order(1)
  public void getSchemaRequestsCountsForMyRequestsTeam1() {
    Map<String, Map<String, Long>> results =
        selectDataJdbc.getSchemaRequestsCounts(101, RequestMode.MY_REQUESTS, 101);

    Map<String, Long> statsCount = results.get("STATUS_COUNTS");
    Map<String, Long> operationTypeCount = results.get("OPERATION_TYPE_COUNTS");

    assertThat(results.size()).isEqualTo(2);
    assertThat(statsCount.get(RequestStatus.CREATED.value)).isEqualTo(5L);
    assertThat(statsCount.get(RequestStatus.APPROVED.value)).isEqualTo(3L);
    assertThat(statsCount.get(RequestStatus.DECLINED.value)).isEqualTo(7L);
    assertThat(statsCount.get(RequestStatus.DELETED.value)).isEqualTo(2L);
    assertThat(operationTypeCount.get(RequestOperationType.CREATE.value)).isEqualTo(15L);
  }

  @Test
  @Order(2)
  public void getSchemaRequestsCountsForApproveForTeam1() {
    Map<String, Map<String, Long>> results =
        selectDataJdbc.getSchemaRequestsCounts(101, RequestMode.TO_APPROVE, 101);

    Map<String, Long> statsCount = results.get("STATUS_COUNTS");
    Map<String, Long> operationTypeCount = results.get("OPERATION_TYPE_COUNTS");

    assertThat(results.size()).isEqualTo(2);
    assertThat(statsCount.get(RequestStatus.CREATED.value)).isEqualTo(5L);
    assertThat(statsCount.get(RequestStatus.APPROVED.value)).isEqualTo(3L);
    assertThat(statsCount.get(RequestStatus.DECLINED.value)).isEqualTo(7L);
    assertThat(statsCount.get(RequestStatus.DELETED.value)).isEqualTo(2L);
    assertThat(operationTypeCount.get(RequestOperationType.CREATE.value)).isEqualTo(15L);
  }

  @Test
  @Order(3)
  public void getSchemaRequestsCountsForApproveForTeam2() {
    Map<String, Map<String, Long>> results =
        selectDataJdbc.getSchemaRequestsCounts(103, RequestMode.TO_APPROVE, 101);

    Map<String, Long> statsCount = results.get("STATUS_COUNTS");
    Map<String, Long> operationTypeCount = results.get("OPERATION_TYPE_COUNTS");

    assertThat(results.size()).isEqualTo(2);
    assertThat(statsCount.get(RequestStatus.CREATED.value)).isEqualTo(4L);
    assertThat(statsCount.get(RequestStatus.APPROVED.value)).isEqualTo(0L);
    assertThat(statsCount.get(RequestStatus.DECLINED.value)).isEqualTo(0L);
    assertThat(statsCount.get(RequestStatus.DELETED.value)).isEqualTo(0L);
    assertThat(operationTypeCount.get(RequestOperationType.CREATE.value)).isEqualTo(0L);
  }

  @Test
  @Order(3)
  public void getSchemaRequestsCountsForMyRequestsForTeam2() {
    Map<String, Map<String, Long>> results =
        selectDataJdbc.getSchemaRequestsCounts(103, RequestMode.MY_REQUESTS, 101);

    Map<String, Long> statsCount = results.get("STATUS_COUNTS");
    Map<String, Long> operationTypeCount = results.get("OPERATION_TYPE_COUNTS");

    assertThat(results.size()).isEqualTo(2);
    assertThat(statsCount.get(RequestStatus.CREATED.value)).isEqualTo(4L);
    assertThat(statsCount.get(RequestStatus.APPROVED.value)).isEqualTo(0L);
    assertThat(statsCount.get(RequestStatus.DECLINED.value)).isEqualTo(0L);
    assertThat(statsCount.get(RequestStatus.DELETED.value)).isEqualTo(0L);
    assertThat(operationTypeCount.get(RequestOperationType.CREATE.value)).isEqualTo(0L);
  }

  @Test
  @Order(4)
  public void givenAllReqsTrueNoFilterOptionsOnlyReturnCreatedByOthersOnMyTeam() {

    List<SchemaRequest> results =
        selectDataJdbc.selectFilteredSchemaRequests(
            true, "James", 101, null, null, null, null, false);

    for (SchemaRequest req : results) {
      assertThat(req.getTopicstatus()).isEqualTo(RequestStatus.CREATED.value);
      assertThat(req.getUsername()).isNotEqualTo("James");
      assertThat(req.getTeamId()).isEqualTo(101);
    }
  }

  @Test
  @Order(5)
  public void givenAllReqsFalseNoFilterOptionsOnlyReturnCreatedByOthersOnMyTeam() {

    List<SchemaRequest> results =
        selectDataJdbc.selectFilteredSchemaRequests(
            false, "James", 101, null, null, null, null, false);

    for (SchemaRequest req : results) {
      // All Statuses allowed
      assertThat(req.getTopicstatus())
          .containsAnyOf(
              RequestStatus.CREATED.value,
              RequestStatus.APPROVED.value,
              RequestStatus.DECLINED.value,
              RequestStatus.DELETED.value);
      assertThat(req.getUsername()).isNotEqualTo("James");
      assertThat(req.getTeamId()).isEqualTo(101);
    }
  }

  @Test
  @Order(6)
  public void givenAllReqsTrueStatusFilterOptionsOnlyReturnDeclinedByOthersOnMyTeam() {

    List<SchemaRequest> results =
        selectDataJdbc.selectFilteredSchemaRequests(
            true, "James", 101, null, null, RequestStatus.DECLINED.value, null, false);

    for (SchemaRequest req : results) {
      // All Statuses allowed
      assertThat(req.getTopicstatus()).containsAnyOf(RequestStatus.DECLINED.value);
      assertThat(req.getUsername()).isNotEqualTo("James");
      assertThat(req.getTeamId()).isEqualTo(101);
    }
  }

  @Test
  @Order(7)
  public void givenAllReqsTrueStatusFilterOptionsOnlyReturnApprovedByOthersOnMyTeam() {

    List<SchemaRequest> results =
        selectDataJdbc.selectFilteredSchemaRequests(
            true, "James", 101, null, null, RequestStatus.APPROVED.value, null, false);

    for (SchemaRequest req : results) {
      // All Statuses allowed
      assertThat(req.getTopicstatus()).containsAnyOf(RequestStatus.APPROVED.value);
      assertThat(req.getUsername()).isNotEqualTo("James");
      assertThat(req.getTeamId()).isEqualTo(101);
    }
  }

  @Test
  @Order(8)
  public void givenAllReqsFalseStatusFilterOptionsOnlyReturnDeclinedByOthersOnMyTeam() {

    List<SchemaRequest> results =
        selectDataJdbc.selectFilteredSchemaRequests(
            false, "James", 101, null, null, RequestStatus.DECLINED.value, null, false);

    for (SchemaRequest req : results) {
      // All Statuses allowed
      assertThat(req.getTopicstatus()).containsAnyOf(RequestStatus.DECLINED.value);
      assertThat(req.getUsername()).isNotEqualTo("James");
      assertThat(req.getTeamId()).isEqualTo(101);
    }
  }

  @Test
  @Order(9)
  public void givenAllReqsFalseStatusFilterOptionsOnlyReturnApprovedByOthersOnMyTeam() {

    List<SchemaRequest> results =
        selectDataJdbc.selectFilteredSchemaRequests(
            false, "James", 101, null, null, RequestStatus.APPROVED.value, null, false);

    for (SchemaRequest req : results) {
      // All Statuses allowed
      assertThat(req.getTopicstatus()).containsAnyOf(RequestStatus.APPROVED.value);
      assertThat(req.getUsername()).isNotEqualTo("James");
      assertThat(req.getTeamId()).isEqualTo(101);
    }
  }

  @Test
  @Order(10)
  public void
      givenAllReqsTrueStatusFilterOptionsOnlyReturnMatchingSpecificWildcardByOthersOnMyTeam() {

    List<SchemaRequest> results =
        selectDataJdbc.selectFilteredSchemaRequests(
            true, "James", 101, null, null, RequestStatus.ALL.value, "topic1", false);

    for (SchemaRequest req : results) {
      // firsttopic1 is the noly one that should match.
      assertThat(req.getTopicname()).containsAnyOf("firsttopic1", "firsttopic10");
      assertThat(req.getUsername()).isNotEqualTo("James");
      assertThat(req.getTeamId()).isEqualTo(101);
    }
  }

  @Test
  @Order(11)
  public void
      givenAllReqsTrueStatusFilterOptionsOnlyReturnMatchingGeneralWildcardByOthersOnMyTeam() {

    List<SchemaRequest> results =
        selectDataJdbc.selectFilteredSchemaRequests(
            true, "James", 101, null, null, RequestStatus.ALL.value, "topic", false);

    for (SchemaRequest req : results) {
      // firstopic5 was created by james so should not be returned.
      assertThat(req.getTopicname()).isNotEqualTo("firsttopic5");
      assertThat(req.getUsername()).isNotEqualTo("James");
      assertThat(req.getTeamId()).isEqualTo(101);
    }
  }

  @Test
  @Order(12)
  public void givenAllReqsFalseStatusFilterOptionsIgnoreWildcardByOthersOnMyTeam() {

    List<SchemaRequest> results =
        selectDataJdbc.selectFilteredSchemaRequests(
            false, "James", 101, null, null, RequestStatus.ALL.value, "topic1", false);
    assertThat(results.size()).isEqualTo(17);
    for (SchemaRequest req : results) {
      // All Statuses allowed
      assertThat(req.getTopicstatus())
          .containsAnyOf(
              RequestStatus.CREATED.value,
              RequestStatus.APPROVED.value,
              RequestStatus.DECLINED.value,
              RequestStatus.DELETED.value);
      assertThat(req.getUsername()).isNotEqualTo("James");
      assertThat(req.getTeamId()).isEqualTo(101);
    }
  }

  @Test
  @Order(13)
  public void givenAllReqsFalseStatusFilterOptionsIgnoreMatchingWildcardByOthersOnMyTeam() {

    List<SchemaRequest> results =
        selectDataJdbc.selectFilteredSchemaRequests(
            false, "James", 101, null, null, RequestStatus.ALL.value, "topic2", false);
    assertThat(results.size()).isEqualTo(17);
    for (SchemaRequest req : results) {
      // All Statuses allowed
      assertThat(req.getTopicstatus())
          .containsAnyOf(
              RequestStatus.CREATED.value,
              RequestStatus.APPROVED.value,
              RequestStatus.DECLINED.value,
              RequestStatus.DELETED.value);
      assertThat(req.getUsername()).isNotEqualTo("James");
      assertThat(req.getTeamId()).isEqualTo(101);
    }
  }

  @Test
  @Order(14)
  public void givenAllReqsTrueStatusFilterOptionsOnlyReturnMatchingEnvByOthersOnMyTeam() {

    List<SchemaRequest> results =
        selectDataJdbc.selectFilteredSchemaRequests(
            true, "James", 101, null, "dev", RequestStatus.ALL.value, null, false);

    for (SchemaRequest req : results) {
      // firstopic5 was created by james so should not be returned.
      assertThat(req.getTopicname()).isNotEqualTo("firsttopic5");
      assertThat(req.getUsername()).isNotEqualTo("James");
      assertThat(req.getTeamId()).isEqualTo(101);
    }
  }

  @Test
  @Order(15)
  public void givenAllReqsFalseStatusFilterOptionsIgnoreDevEnvFilterOptionByOthersOnMyTeam() {

    List<SchemaRequest> results =
        selectDataJdbc.selectFilteredSchemaRequests(
            false, "James", 101, null, "dev", RequestStatus.ALL.value, null, false);
    assertThat(results.size()).isEqualTo(17);
    for (SchemaRequest req : results) {
      // All Statuses allowed
      assertThat(req.getTopicstatus())
          .containsAnyOf(
              RequestStatus.CREATED.value,
              RequestStatus.APPROVED.value,
              RequestStatus.DECLINED.value,
              RequestStatus.DELETED.value);
      assertThat(req.getUsername()).isNotEqualTo("James");
      assertThat(req.getTeamId()).isEqualTo(101);
    }
  }

  @Test
  @Order(16)
  public void givenAllReqsTrueStatusFilterOptionsOnlyReturnMatchingSpecificTopicByOthersOnMyTeam() {

    List<SchemaRequest> results =
        selectDataJdbc.selectFilteredSchemaRequests(
            true, "James", 101, "firsttopic10", null, RequestStatus.ALL.value, null, false);

    for (SchemaRequest req : results) {
      // firsttopic1 is the noly one that should match.
      assertThat(req.getTopicname()).isEqualTo("firsttopic10");
      assertThat(req.getUsername()).isNotEqualTo("James");
      assertThat(req.getTeamId()).isEqualTo(101);
    }
  }

  @Test
  @Order(17)
  public void givenReqsReturnMyRequestsOnlyForTSingleTopic() {

    List<SchemaRequest> results =
        selectDataJdbc.selectFilteredSchemaRequests(
            false, "John", 101, "firsttopic5", null, RequestStatus.ALL.value, null, true);
    assertThat(results.size()).isEqualTo(0);
  }

  @Test
  @Order(18)
  public void givenReqsReturnMyRequestsOnly() {

    List<SchemaRequest> results =
        selectDataJdbc.selectFilteredSchemaRequests(
            false, "Jackie", 101, null, null, RequestStatus.ALL.value, null, true);
    assertThat(results.size()).isEqualTo(17);
    for (SchemaRequest req : results) {
      assertThat(req.getUsername()).isEqualTo("Jackie");
      assertThat(req.getTeamId()).isEqualTo(101);
    }
  }

  @Test
  @Order(19)
  public void givenReqsReturnMyRequestsOnlyFromDevEnv() {

    List<SchemaRequest> results =
        selectDataJdbc.selectFilteredSchemaRequests(
            false, "Jackie", 101, null, "dev", RequestStatus.ALL.value, null, true);
    assertThat(results.size()).isEqualTo(17);
    for (SchemaRequest req : results) {
      assertThat(req.getUsername()).isEqualTo("Jackie");
      assertThat(req.getTeamId()).isEqualTo(101);
    }
  }

  @Test
  @Order(20)
  public void givenReqsReturnMyRequestsOnlyFromTestEnv() {

    List<SchemaRequest> results =
        selectDataJdbc.selectFilteredSchemaRequests(
            false, "Jackie", 101, null, "test", RequestStatus.ALL.value, null, true);
    assertThat(results.size()).isEqualTo(0);
  }

  @Test
  @Order(21)
  public void givenReqsReturnAllRequestsOnlyFromDevEnv() {

    List<SchemaRequest> results =
        selectDataJdbc.selectFilteredSchemaRequests(
            false, "John", 101, null, "dev", RequestStatus.ALL.value, null, false);
    assertThat(results.size()).isEqualTo(4);
    for (SchemaRequest req : results) {
      assertThat(req.getUsername()).isNotEqualTo("John");
      assertThat(req.getTeamId()).isEqualTo(103);
    }
  }

  @Test
  @Order(22)
  public void givenReqsReturnAllRequests() {

    List<SchemaRequest> results =
        selectDataJdbc.selectFilteredSchemaRequests(
            false, "John", 101, null, null, RequestStatus.ALL.value, null, false);
    assertThat(results.size()).isEqualTo(4);
  }

  @Test
  @Order(23)
  public void givenReqsReturnAllRequestsForTeam101() {

    List<SchemaRequest> results =
        selectDataJdbc.selectFilteredSchemaRequests(
            false, "James", 101, null, null, RequestStatus.ALL.value, null, false);
    assertThat(results.size()).isEqualTo(17);
  }

  private void generateData(
      int number,
      int teamId,
      int tenantId,
      String topicName,
      String env,
      RequestStatus requestStatus,
      RequestOperationType requestOperationType,
      int topicIdentifier,
      String requestor) {

    for (int i = 0; i < number; i++) {
      SchemaRequest schemaRequest = new SchemaRequest();
      schemaRequest.setTenantId(tenantId);
      schemaRequest.setTeamId(teamId);
      schemaRequest.setTopicname(topicName + "" + i);
      schemaRequest.setEnvironment(env);
      schemaRequest.setTopicstatus(requestStatus.value);
      schemaRequest.setRequesttype(requestOperationType.value);
      schemaRequest.setReq_no(topicIdentifier + i);
      schemaRequest.setSchemafull("{schema}");
      schemaRequest.setForceRegister(false);
      schemaRequest.setUsername(requestor);
      entityManager.persistAndFlush(schemaRequest);
    }
  }
}
