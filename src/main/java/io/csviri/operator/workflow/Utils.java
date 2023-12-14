package io.csviri.operator.workflow;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.csviri.operator.workflow.customresource.workflow.DependentResourceSpec;
import io.csviri.operator.workflow.customresource.workflow.Workflow;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.javaoperatorsdk.operator.api.reconciler.Context;

public class Utils {

  private Utils() {}

  // todo optimize? cache name by uid?
  public static Map<String, GenericKubernetesResource> getActualResourcesByName(
      Context<Workflow> context, Workflow workflow) {
    var secondaryResources = context.getSecondaryResources(GenericKubernetesResource.class);
    Map<String, GenericKubernetesResource> res = new HashMap<>();
    secondaryResources.forEach(sr -> {
      var drSpec = workflow.getSpec().getResources().stream()
          .filter(r -> Utils.getApiVersion(r).equals(sr.getApiVersion())
              && Utils.getApiVersion(r).equals(sr.getKind())
              && Utils.getName(r).equals(sr.getMetadata().getName())
      // todo handle namespaces properly
      // && Objects.equals(r.getResource().getMetadata().getNamespace(),
      // sr.getMetadata().getNamespace()))
      ).findFirst();
      var name = drSpec.map(DependentResourceSpec::getName).orElseGet(() -> nameResource(sr));
      res.put(name, sr);
    });
    return res;
  }

  public static String nameResource(GenericKubernetesResource resource) {
    return resource.getApiVersion() + "#"
        + resource.getKind() + "#"
        + resource.getMetadata().getName()
        + (resource.getMetadata().getNamespace() == null ? ""
            : ("#" + resource.getMetadata().getNamespace()));
  }

  public static String getName(DependentResourceSpec spec) {
    if (spec.getResource() != null) {
      return spec.getResource().getMetadata().getName();
    } else {
      return getPropertyValueFromTemplate(spec.getResourceTemplate(), "name");
    }
  }

  public static String getApiVersion(DependentResourceSpec spec) {
    if (spec.getResource() != null) {
      return spec.getResource().getApiVersion();
    } else {
      return getPropertyValueFromTemplate(spec.getResourceTemplate(), "apiVersion");
    }
  }

  public static String getKind(DependentResourceSpec spec) {
    if (spec.getResource() != null) {
      return spec.getResource().getKind();
    } else {
      return getPropertyValueFromTemplate(spec.getResourceTemplate(), "kind");
    }
  }

  public static Optional<String> getNamespace(DependentResourceSpec spec) {
    if (spec.getResource() != null) {
      return Optional.ofNullable(spec.getResource().getMetadata().getNamespace());
    } else {
      return getOptionalPropertyValueFromTemplate(spec.getResourceTemplate(), "namespace");
    }
  }

  public static String getApiVersionFromTemplate(String resourceTemplate) {
    return getPropertyValueFromTemplate(resourceTemplate, "apiVersion");
  }

  public static String getKindFromTemplate(String resourceTemplate) {
    return getPropertyValueFromTemplate(resourceTemplate, "kind");
  }

  private static Optional<String> getOptionalPropertyValueFromTemplate(String resourceTemplate,
      String property) {
    var finalProp = property + ":";
    var targetLine = resourceTemplate.lines().filter(l -> l.contains(finalProp)).findFirst();
    return targetLine.map(l -> l.replace(finalProp, "").trim());
  }

  private static String getPropertyValueFromTemplate(String resourceTemplate, String property) {
    return getOptionalPropertyValueFromTemplate(resourceTemplate, property)
        .orElseThrow(() -> new IllegalArgumentException(
            "Template does not contain property. " + resourceTemplate));
  }

}
