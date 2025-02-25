package io.aiven.klaw.dao;

import io.aiven.klaw.model.enums.AclIPPrincipleType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
@Entity
@IdClass(AclID.class)
@Table(name = "kwacls")
public class Acl implements Serializable {

  @Id
  @Column(name = "aclid")
  private Integer req_no;

  @Id
  @Column(name = "tenantid")
  private Integer tenantId;

  @Column(name = "topicname")
  private String topicname;

  @Column(name = "env")
  private String environment;

  @Column(name = "teamid")
  private Integer teamId;

  @Column(name = "consumergroup")
  private String consumergroup;

  // producer or consumer
  @Column(name = "topictype")
  private String topictype;

  @Column(name = "aclip")
  private String aclip;

  @Column(name = "aclssl")
  private String aclssl;

  // prefixed acls or Literal(default)
  @Column(name = "aclpatterntype")
  private String aclPatternType;

  // Topic always
  @Column(name = "aclresourcetype")
  private String aclResourceType;

  // TransactionalID
  @Column(name = "transactionalid")
  private String transactionalId;

  @Column(name = "otherparams")
  private String otherParams;

  @Column(name = "jsonparams")
  private String jsonParams;

  @Column(name = "aclipprincipletype")
  private AclIPPrincipleType aclIpPrincipleType;
}
