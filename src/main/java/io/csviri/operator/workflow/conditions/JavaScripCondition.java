package io.csviri.operator.workflow.conditions;

import javax.script.*;

import io.csviri.operator.workflow.GenericDependentResource;
import io.csviri.operator.workflow.WorkflowException;
import io.csviri.operator.workflow.customresource.workflow.Workflow;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

public class JavaScripCondition implements Condition<GenericKubernetesResource, Workflow> {

  private String condition;

  public JavaScripCondition(String condition) {
    this.condition = condition;
  }

  @Override
  public boolean isMet(DependentResource<GenericKubernetesResource, Workflow> dependentResource,
      Workflow workflow,
      Context<Workflow> context) {
    try {
      ScriptEngineManager manager = new ScriptEngineManager();
      ScriptEngine engine = manager.getEngineByName("js");

      var resources = context.getSecondaryResources(GenericDependentResource.class).toArray();
      engine.put("secondary", resources);

      var actual = dependentResource.getSecondaryResource(workflow, context).orElseThrow();
      engine.put("actual", Serialization.asJson(actual));

      CompiledScript script = ((Compilable) engine).compile(condition);
      return (boolean) script.eval();

    } catch (ScriptException e) {
      throw new WorkflowException(e);
    }
  }

}
