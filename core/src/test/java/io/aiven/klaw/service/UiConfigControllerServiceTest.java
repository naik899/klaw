package io.aiven.klaw.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.aiven.klaw.UtilMethods;
import io.aiven.klaw.config.ManageDatabase;
import io.aiven.klaw.dao.ActivityLog;
import io.aiven.klaw.dao.Env;
import io.aiven.klaw.dao.KwClusters;
import io.aiven.klaw.dao.Team;
import io.aiven.klaw.dao.UserInfo;
import io.aiven.klaw.helpers.db.rdbms.HandleDbRequestsJdbc;
import io.aiven.klaw.model.EnvModel;
import io.aiven.klaw.model.UserInfoModel;
import io.aiven.klaw.model.enums.KafkaClustersType;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(SpringExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UiConfigControllerServiceTest {

  @Mock private HandleDbRequestsJdbc handleDbRequests;

  @Mock private ClusterApiService clusterApiService;

  @Mock private MailUtils mailService;

  @Mock private UserInfo userInfo;

  @Mock private UserInfoModel userInfoModel;

  @Mock private UserDetails userDetails;

  @Mock private ManageDatabase manageDatabase;

  @Mock CommonUtilsService commonUtilsService;

  @Mock private InMemoryUserDetailsManager inMemoryUserDetailsManager;

  @Mock private Map<Integer, String> tenantMap;

  @Mock private Map<Integer, KwClusters> kwClustersHashMap;

  @Mock private KwClusters kwClusters;

  private EnvModel env;

  private EnvsClustersTenantsControllerService envsClustersTenantsControllerService;

  private UsersTeamsControllerService usersTeamsControllerService;

  private UiConfigControllerService uiConfigControllerService;

  @BeforeEach
  public void setUp() throws Exception {
    usersTeamsControllerService = new UsersTeamsControllerService();
    envsClustersTenantsControllerService = new EnvsClustersTenantsControllerService();
    envsClustersTenantsControllerService.setServices(clusterApiService, mailService);

    this.env = new EnvModel();
    env.setName("DEV");
    ReflectionTestUtils.setField(
        usersTeamsControllerService, "inMemoryUserDetailsManager", inMemoryUserDetailsManager);
    ReflectionTestUtils.setField(
        envsClustersTenantsControllerService, "manageDatabase", manageDatabase);
    ReflectionTestUtils.setField(
        envsClustersTenantsControllerService, "commonUtilsService", commonUtilsService);
    when(manageDatabase.getHandleDbRequests()).thenReturn(handleDbRequests);
    loginMock();
  }

  private void loginMock() {
    Authentication authentication = Mockito.mock(Authentication.class);
    SecurityContext securityContext = Mockito.mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(userDetails);
    SecurityContextHolder.setContext(securityContext);
  }

  @Test
  @Order(1)
  public void getEnvs1() {

    stubUserInfo();
    when(mailService.getEnvProperty(anyInt(), anyString())).thenReturn("1");
    when(manageDatabase.getKafkaEnvList(anyInt())).thenReturn(getAllEnvs());
    when(commonUtilsService.isNotAuthorizedUser(any(), any())).thenReturn(false);
    when(manageDatabase.getTenantMap()).thenReturn(tenantMap);
    when(tenantMap.get(anyInt())).thenReturn("1");
    when(manageDatabase.getClusters(any(KafkaClustersType.class), anyInt()))
        .thenReturn(kwClustersHashMap);
    when(kwClustersHashMap.get(anyInt())).thenReturn(kwClusters);

    List<EnvModel> envsList = envsClustersTenantsControllerService.getKafkaEnvs();

    assertThat(envsList).hasSize(3);
    assertThat(envsList.get(0).getEnvStatus()).isNull();
  }

  @Test
  @Order(4)
  public void getSchemaRegEnvs() {
    stubUserInfo();
    when(commonUtilsService.isNotAuthorizedUser(any(), any())).thenReturn(false);

    when(handleDbRequests.selectAllSchemaRegEnvs(1)).thenReturn(getAllSchemaEnvs());
    List<EnvModel> envsList = envsClustersTenantsControllerService.getSchemaRegEnvs();

    assertThat(envsList).isEmpty();
  }

  @Test
  @Order(5)
  public void getRequestSchemaEnvs_IsEmpty() {
    stubUserInfo();
    when(commonUtilsService.isNotAuthorizedUser(any(), any())).thenReturn(false);
    when(mailService.getEnvProperty(eq(101), eq("ORDER_OF_SCHEMA_ENVS"))).thenReturn("DEV,TST");
    when(mailService.getEnvProperty(eq(101), eq("REQUEST_SCHEMA_OF_ENVS"))).thenReturn("");
    when(handleDbRequests.selectAllSchemaRegEnvs(1)).thenReturn(getAllSchemaEnvs());
    when(manageDatabase.getSchemaRegEnvList(eq(101))).thenReturn(getAllSchemaEnvs());
    List<EnvModel> envsList = envsClustersTenantsControllerService.getEnvsForSchemaRequests();

    assertThat(envsList).isEmpty();
  }

  @Test
  @Order(6)
  public void getRequestSchemaEnvs_ReturnDevEnv() {

    stubUserInfo();
    when(commonUtilsService.isNotAuthorizedUser(any(), any())).thenReturn(false);
    when(mailService.getEnvProperty(eq(101), eq("ORDER_OF_SCHEMA_ENVS"))).thenReturn("DEV,TST");
    when(mailService.getEnvProperty(eq(101), eq("REQUEST_SCHEMA_OF_ENVS"))).thenReturn("DEV");
    when(handleDbRequests.selectAllSchemaRegEnvs(1)).thenReturn(getAllSchemaEnvs());
    when(manageDatabase.getSchemaRegEnvList(eq(101))).thenReturn(getAllSchemaEnvs());
    when(manageDatabase.getClusters(eq(KafkaClustersType.SCHEMA_REGISTRY), eq(101)))
        .thenReturn(getSchemaRegistryClusters());
    List<EnvModel> envsList = envsClustersTenantsControllerService.getEnvsForSchemaRequests();

    assertThat(envsList.get(0).getName()).isEqualTo("DEV");
    assertThat(envsList.size()).isEqualTo(1);
  }

  @Test
  @Order(7)
  public void getRequestSchemaEnvs_GivenWrongSchemaEnvNameReturnDevEnvOnly() {
    // sTT is a misspelt env one tht does not exist and so should not be returned.
    stubUserInfo();
    when(commonUtilsService.isNotAuthorizedUser(any(), any())).thenReturn(false);
    when(mailService.getEnvProperty(eq(101), eq("ORDER_OF_SCHEMA_ENVS"))).thenReturn("DEV,TST");
    when(mailService.getEnvProperty(eq(101), eq("REQUEST_SCHEMA_OF_ENVS"))).thenReturn("DEV,sTT");
    when(handleDbRequests.selectAllSchemaRegEnvs(1)).thenReturn(getAllSchemaEnvs());
    when(manageDatabase.getSchemaRegEnvList(eq(101))).thenReturn(getAllSchemaEnvs());
    when(manageDatabase.getClusters(eq(KafkaClustersType.SCHEMA_REGISTRY), eq(101)))
        .thenReturn(getSchemaRegistryClusters());
    List<EnvModel> envsList = envsClustersTenantsControllerService.getEnvsForSchemaRequests();

    assertThat(envsList.get(0).getName()).isEqualTo("DEV");
    assertThat(envsList.size()).isEqualTo(1);
  }

  @Test
  @Order(8)
  public void getRequestSchemaEnvs_GivenTwoSchemaEnvsReturnBoth() {
    // DEV and TSTS are both spelt correctly and configured so both should be returned.
    stubUserInfo();
    when(commonUtilsService.isNotAuthorizedUser(any(), any())).thenReturn(false);
    when(mailService.getEnvProperty(eq(101), eq("ORDER_OF_SCHEMA_ENVS"))).thenReturn("DEV,TST");
    when(mailService.getEnvProperty(eq(101), eq("REQUEST_SCHEMA_OF_ENVS"))).thenReturn("DEV,TST");
    when(handleDbRequests.selectAllSchemaRegEnvs(1)).thenReturn(getAllSchemaEnvs());
    when(manageDatabase.getSchemaRegEnvList(eq(101))).thenReturn(getAllSchemaEnvs());
    when(manageDatabase.getClusters(eq(KafkaClustersType.SCHEMA_REGISTRY), eq(101)))
        .thenReturn(getSchemaRegistryClusters());
    List<EnvModel> envsList = envsClustersTenantsControllerService.getEnvsForSchemaRequests();

    assertThat(envsList.get(0).getName()).isEqualTo("DEV");
    assertThat(envsList.get(1).getName()).isEqualTo("TST");
    assertThat(envsList.size()).isEqualTo(2);
  }

  @Test
  @Order(9)
  public void getRequestSchemaEnvs_GivenThreeSchemaEnvsReturnOnlyTheTwoConfigured() {
    // only two kWclusters are configured DEV and TST so UAT should not return.
    stubUserInfo();
    when(commonUtilsService.isNotAuthorizedUser(any(), any())).thenReturn(false);
    when(mailService.getEnvProperty(eq(101), eq("ORDER_OF_SCHEMA_ENVS"))).thenReturn("DEV,TST,UAT");
    when(mailService.getEnvProperty(eq(101), eq("REQUEST_SCHEMA_OF_ENVS")))
        .thenReturn("DEV,TST,UAT");
    when(handleDbRequests.selectAllSchemaRegEnvs(1)).thenReturn(getAllSchemaEnvs());
    when(manageDatabase.getSchemaRegEnvList(eq(101))).thenReturn(getAllSchemaEnvs());
    when(manageDatabase.getClusters(eq(KafkaClustersType.SCHEMA_REGISTRY), eq(101)))
        .thenReturn(getSchemaRegistryClusters());
    List<EnvModel> envsList = envsClustersTenantsControllerService.getEnvsForSchemaRequests();

    assertThat(envsList.get(0).getName()).isEqualTo("DEV");
    assertThat(envsList.get(1).getName()).isEqualTo("TST");
    assertThat(envsList.size()).isEqualTo(2);
  }

  private Map<Integer, KwClusters> getSchemaRegistryClusters() {
    Map<Integer, KwClusters> map = new HashMap<>();
    UtilMethods util = new UtilMethods();
    map.put(1, util.getKwClusters());
    map.put(4, util.getKwClusters());
    return map;
  }

  private List<ActivityLog> getAcitivityList(int size) {
    List<ActivityLog> actList = new ArrayList<>();

    if (size > 0) {

      ActivityLog actLog1 = new ActivityLog();
      actLog1.setEnv("DEV");
      actLog1.setActivityTime(new Timestamp(System.currentTimeMillis()));
      actList.add(actLog1);

      ActivityLog actLog2 = new ActivityLog();
      actLog2.setEnv("DEV");
      actLog2.setActivityTime(new Timestamp(System.currentTimeMillis()));
      actList.add(actLog2);
    }
    return actList;
  }

  private List<UserInfo> getUsernfoList() {
    List<UserInfo> listUsersInfo = new ArrayList<>();

    UserInfo userInfo = new UserInfo();
    userInfo.setUsername("user1");

    listUsersInfo.add(userInfo);

    return listUsersInfo;
  }

  private List<Team> getAvailableTeams() {

    Team team1 = new Team();
    team1.setTeamname("Team1");

    List<Team> teamList = new ArrayList<>();
    teamList.add(team1);

    return teamList;
  }

  private List<Team> getAvailableTeamsSU() {

    Team team1 = new Team();
    team1.setTeamname("Team1");

    Team team2 = new Team();
    team2.setTeamname("Team2");

    Team team3 = new Team();
    team3.setTeamname("Team3");

    List<Team> teamList = new ArrayList<>();
    teamList.add(team1);
    teamList.add(team2);
    teamList.add(team3);

    return teamList;
  }

  private List<Env> getAllSchemaEnvs() {
    List<Env> listEnvs = new ArrayList<>();

    Env env = new Env();
    env.setName("DEV");
    env.setId("DEV");
    env.setClusterId(1);
    listEnvs.add(env);

    env = new Env();
    env.setName("TST");
    env.setId("TST");
    env.setClusterId(4);
    listEnvs.add(env);

    return listEnvs;
  }

  private List<Env> getAllEnvs() {
    List<Env> listEnvs = new ArrayList<>();

    Env env = new Env();
    env.setId("1");
    env.setName("DEV");
    env.setTenantId(101);
    env.setClusterId(101);
    listEnvs.add(env);

    Env env1 = new Env();
    env1.setId("2");
    env1.setClusterId(101);
    env1.setTenantId(101);
    env1.setName("TST");
    listEnvs.add(env1);

    Env env2 = new Env();
    env2.setId("3");
    env2.setClusterId(101);
    env2.setName("ACC");
    env2.setTenantId(101);
    listEnvs.add(env2);

    return listEnvs;
  }

  private void stubUserInfo() {
    when(handleDbRequests.getUsersInfo(anyString())).thenReturn(userInfo);
    when(userInfo.getTeamId()).thenReturn(101);
    when(userInfo.getTenantId()).thenReturn(101);
    when(mailService.getUserName(any())).thenReturn("kwusera");
  }
}
