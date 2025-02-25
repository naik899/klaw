package io.aiven.klaw.model;

import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class TopicOverview extends ResourceOverviewAttributes {
  List<TopicInfo> topicInfoList;
  List<AclInfo> aclInfoList;
  List<AclInfo> prefixedAclInfoList;
  List<AclInfo> transactionalAclInfoList;
  private List<TopicHistory> topicHistoryList;
  Map<String, String> topicPromotionDetails;

  String topicDocumentation;
  Integer topicIdForDocumentation;
}
