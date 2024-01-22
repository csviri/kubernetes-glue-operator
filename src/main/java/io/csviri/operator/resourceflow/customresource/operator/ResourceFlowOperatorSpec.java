package io.csviri.operator.resourceflow.customresource.operator;

import java.util.Objects;

import io.csviri.operator.resourceflow.customresource.resourceflow.ResourceFlowSpec;
import io.fabric8.generator.annotation.Required;

public class ResourceFlowOperatorSpec extends ResourceFlowSpec {

  @Required
  private Parent parent;

  public Parent getParent() {
    return parent;
  }

  public ResourceFlowOperatorSpec setParent(Parent parent) {
    this.parent = parent;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    if (!super.equals(o))
      return false;
    ResourceFlowOperatorSpec that = (ResourceFlowOperatorSpec) o;
    return Objects.equals(parent, that.parent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), parent);
  }

}
