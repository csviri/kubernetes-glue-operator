package io.csviri.operator.resourceglue.customresource.operator;

import java.util.Objects;

import io.csviri.operator.resourceglue.customresource.glue.ResourceGlueSpec;
import io.fabric8.generator.annotation.Required;

public class ResourceGlueOperatorSpec extends ResourceGlueSpec {

  @Required
  private Parent parent;

  public Parent getParent() {
    return parent;
  }

  public ResourceGlueOperatorSpec setParent(Parent parent) {
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
    ResourceGlueOperatorSpec that = (ResourceGlueOperatorSpec) o;
    return Objects.equals(parent, that.parent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), parent);
  }

}
