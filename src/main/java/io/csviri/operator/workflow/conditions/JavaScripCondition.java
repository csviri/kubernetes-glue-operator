package io.csviri.operator.workflow.conditions;

import java.util.Map;
import java.util.stream.Collectors;

import javax.script.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.csviri.operator.workflow.Utils;
import io.csviri.operator.workflow.WorkflowException;
import io.csviri.operator.workflow.customresource.workflow.Workflow;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

public class JavaScripCondition implements Condition<GenericKubernetesResource, Workflow> {

  private static final Logger LOG = LoggerFactory.getLogger(JavaScripCondition.class);

  private static final String RESOURCE_AS_STRING_NAME_SUFFIX = "Str";

  private final String script;

  public JavaScripCondition(String script) {
    this.script = script;
  }

  @Override
  public boolean isMet(DependentResource<GenericKubernetesResource, Workflow> dependentResource,
      Workflow workflow,
      Context<Workflow> context) {
    try {
      ScriptEngineManager manager = new ScriptEngineManager();
      ScriptEngine engine = manager.getEngineByName("js");

      StringBuilder finalScript = new StringBuilder();
      var target = dependentResource.getSecondaryResource(workflow, context);
      target.ifPresent(t -> {
        engine.put("targetStr", Serialization.asJson(t));
        finalScript.append("const target = JSON.parse(targetStr);\n");
      });

      Map<String, String> namedSecondaryResources =
          nameAndSerializeSecondaryResources(context, workflow);
      namedSecondaryResources.forEach((k, v) -> {
        var stringKey = k + RESOURCE_AS_STRING_NAME_SUFFIX;
        engine.put(stringKey, v);
        finalScript.append("const ").append(k).append("= JSON.parse(").append(stringKey)
            .append(");\n");
      });

      finalScript.append("\n").append(script);

      LOG.debug("Final Condition JS:\n{}", finalScript);

      CompiledScript script = ((Compilable) engine).compile(finalScript.toString());
      return (boolean) script.eval();

    } catch (ScriptException e) {
      throw new WorkflowException(e);
    }
  }

  private Map<String, String> nameAndSerializeSecondaryResources(
      Context<Workflow> context, Workflow workflow) {
    return Utils.getActualResourcesByNameInWorkflow(context, workflow).entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> Serialization.asJson(e.getValue())));
  }

}
