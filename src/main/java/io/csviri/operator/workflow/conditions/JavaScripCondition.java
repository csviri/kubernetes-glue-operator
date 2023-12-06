package io.csviri.operator.workflow.conditions;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.script.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.csviri.operator.workflow.WorkflowException;
import io.csviri.operator.workflow.customresource.workflow.Workflow;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

public class JavaScripCondition implements Condition<GenericKubernetesResource, Workflow> {

  private static final Logger LOG = LoggerFactory.getLogger(JavaScripCondition.class);

  private final String STR_SUFFIX = "Str";

  private String script;

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
      target.ifPresent(t->{
        engine.put("targetStr", Serialization.asJson(t));
        finalScript.append("const target = JSON.parse(targetStr);\n");
      });

      Map<String, String> namedSecondaryResources = nameAndSerializeSecondaryResources(
          context.getSecondaryResources(GenericKubernetesResource.class), workflow);
      namedSecondaryResources.forEach((k, v) -> {
        var stringKey = k + STR_SUFFIX;
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

  // optimize? cache name by uid?
  private Map<String, String> nameAndSerializeSecondaryResources(
      Set<GenericKubernetesResource> secondaryResources,
      Workflow workflow) {
    Map<String, String> res = new HashMap<>();
    secondaryResources.forEach(sr -> {
      var drSpec = workflow.getSpec().getResources().stream()
          .filter(r -> r.getResource().getApiVersion().equals(sr.getApiVersion())
              && r.getResource().getKind().equals(sr.getKind())
              && r.getResource().getMetadata().getName().equals(sr.getMetadata().getName())
              && Objects.equals(r.getResource().getMetadata().getNamespace(),
                  sr.getMetadata().getNamespace()))
          .findFirst();
      var name = drSpec.map(s -> s.getName()).orElseGet(() -> nameResource(sr));

      res.put(name, Serialization.asJson(sr));
    });
    return res;
  }

  private String nameResource(GenericKubernetesResource resource) {
    return resource.getApiVersion() + "#"
        + resource.getKind() + "#"
        + resource.getMetadata().getName()
        + (resource.getMetadata().getNamespace() == null ? ""
            : ("#" + resource.getMetadata().getNamespace()));
  }

}
