package io.csviri.operator.glue.customresource.operator;

import java.util.Objects;

import io.csviri.operator.glue.customresource.glue.GlueSpec;
import io.fabric8.generator.annotation.Required;

public class GlueOperatorSpec extends GlueSpec {

  @Required
  private Parent parent;

  public Parent getParent() {
    return parent;
  }

  public GlueOperatorSpec setParent(Parent parent) {
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
    GlueOperatorSpec that = (GlueOperatorSpec) o;
    return Objects.equals(parent, that.parent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), parent);
  }

}
