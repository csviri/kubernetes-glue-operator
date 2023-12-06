package io.csviri.operator.workflow.customresource.workflow;

import java.util.ArrayList;
import java.util.List;

import io.csviri.operator.workflow.customresource.workflow.condition.ConditionSpec;
import io.fabric8.crd.generator.annotation.PreserveUnknownFields;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;

public class DependentResourceSpec {

  private String name;

  // templating, eventually with QUTE
  // private String resourceTemplate;

  @PreserveUnknownFields
  private GenericKubernetesResource resource;

  private List<String> dependsOn = new ArrayList<>();

  @PreserveUnknownFields
  private ConditionSpec readyPostCondition;

  @PreserveUnknownFields
  private ConditionSpec condition;

  @PreserveUnknownFields
  private ConditionSpec deletePostCondition;

  public String getName() {
    return name;
  }

  public DependentResourceSpec setName(String name) {
    this.name = name;
    return this;
  }

  public List<String> getDependsOn() {
    return dependsOn;
  }

  public DependentResourceSpec setDependsOn(List<String> dependsOn) {
    this.dependsOn = dependsOn;
    return this;
  }

  public ConditionSpec getReadyPostCondition() {
    return readyPostCondition;
  }

  public DependentResourceSpec setReadyPostCondition(ConditionSpec readyPostConditionSpec) {
    this.readyPostCondition = readyPostConditionSpec;
    return this;
  }


  public GenericKubernetesResource getResource() {
    return resource;
  }

  public DependentResourceSpec setResource(GenericKubernetesResource resource) {
    this.resource = resource;
    return this;
  }

  public ConditionSpec getCondition() {
    return condition;
  }

  public DependentResourceSpec setCondition(ConditionSpec conditionSpec) {
    this.condition = conditionSpec;
    return this;
  }

  public ConditionSpec getDeletePostCondition() {
    return deletePostCondition;
  }

  public DependentResourceSpec setDeletePostCondition(ConditionSpec deletePostConditionSpec) {
    this.deletePostCondition = deletePostConditionSpec;
    return this;
  }
}
