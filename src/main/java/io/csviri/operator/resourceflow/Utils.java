package io.csviri.operator.resourceflow;

import java.util.*;

import io.csviri.operator.resourceflow.customresource.workflow.DependentResourceSpec;
import io.csviri.operator.resourceflow.customresource.workflow.ResourceFlow;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.GroupVersionKind;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;

public class Utils {

  private Utils() {}

  public static Map<String, GenericKubernetesResource> getActualResourcesByNameInWorkflow(
      Context<ResourceFlow> context, ResourceFlow resourceFlow) {
    var secondaryResources = context.getSecondaryResources(GenericKubernetesResource.class);
    Map<String, GenericKubernetesResource> res = new HashMap<>();
    secondaryResources.forEach(sr -> {
      var dependentSpec = resourceFlow.getSpec().getResources().stream()
          .filter(r -> Utils.getApiVersion(r).equals(sr.getApiVersion())
              && Utils.getKind(r).equals(sr.getKind())
              && Utils.getName(r).equals(sr.getMetadata().getName())
      // todo add to docs
      // namespace not compared here, it should be done it is just not trivial, now it is limited to
      // have one kind of resource in the workflow with the same resource name
      ).findFirst();
      dependentSpec.ifPresent(spec -> res.put(spec.getName(), sr));
    });

    resourceFlow.getSpec().getRelatedResources().forEach(r -> {
      var gvk = new GroupVersionKind(r.getApiVersion(), r.getKind());
      var es =
          (InformerEventSource<GenericKubernetesResource, ResourceFlow>) context
              .eventSourceRetriever()
              .getResourceEventSourceFor(GenericKubernetesResource.class, gvk.toString());
      var namespace =
          r.getNamespace() == null ? resourceFlow.getMetadata().getNamespace() : r.getNamespace();
      if (r.getResourceNames().size() == 1) {
        es.get(new ResourceID(r.getName(), namespace)).ifPresent(resource -> {
          res.put(r.getName(), resource);
        });
      } else {
        r.getResourceNames().forEach(resourceName -> es.get(new ResourceID(r.getName(), namespace))
            .ifPresent(resource -> res.put(r.getName() + "." + resourceName, resource)));
      }
    });

    return res;
  }

  public static GroupVersionKind getGVK(DependentResourceSpec spec) {
    return new GroupVersionKind(getApiVersion(spec), getKind(spec));
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

  public static Set<String> leafResourceNames(ResourceFlow resourceFlow) {
    Set<String> result = new HashSet<>();
    resourceFlow.getSpec().getResources().forEach(r -> result.add(r.getName()));
    resourceFlow.getSpec().getResources().forEach(r -> {
      r.getDependsOn().forEach(result::remove);
    });
    return result;
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
