package io.csviri.operator.workflow.dependent;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.stream.Collectors;

import io.csviri.operator.workflow.Utils;
import io.csviri.operator.workflow.customresource.workflow.Workflow;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.GarbageCollected;
import io.javaoperatorsdk.operator.processing.GroupVersionKind;
import io.javaoperatorsdk.operator.processing.dependent.Creator;
import io.javaoperatorsdk.operator.processing.dependent.Updater;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.GenericKubernetesDependentResource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;

public class GenericDependentResource
    extends GenericKubernetesDependentResource<Workflow> implements GarbageCollected<Workflow>,
    Updater<GenericKubernetesResource, Workflow>,
    Creator<GenericKubernetesResource, Workflow> {

  private static ObjectMapper objectMapper = new ObjectMapper();
  private static MustacheFactory mustacheFactory = new DefaultMustacheFactory();

  private GenericKubernetesResource desired;

  public GenericDependentResource(GenericKubernetesResource desired) {
    super(new GroupVersionKind(desired.getApiVersion(), desired.getKind()));
    this.desired = desired;
  }

  @Override
  protected GenericKubernetesResource desired(Workflow primary,
      Context<Workflow> context) {

    // todo how do I know if a resource is namespaced or not => explicit flag / and check fabric8

    if (desired.getMetadata().getNamespace() == null) {
      desired.getMetadata().setNamespace(primary.getMetadata().getNamespace());
    }


    var template = Serialization.asYaml(desired);
    // this can be precompiled
    var mustache = mustacheFactory.compile(new StringReader(template), "desired");
    // convert GKR to Map for better access ?
    var actualResourcesByName = Utils.getActualResourcesByName(context, primary);
    var mustacheContext = actualResourcesByName.entrySet().stream().collect(Collectors
        .toMap(Map.Entry::getKey, e -> objectMapper.convertValue(e.getValue(), Map.class)));
    var res = mustache.execute(new StringWriter(), mustacheContext);

    var resultDesired = Serialization.unmarshal(res.toString(), GenericKubernetesResource.class);
    return resultDesired;

    // return desired;
  }

  @Override
  public Result<GenericKubernetesResource> match(GenericKubernetesResource actualResource,
      Workflow primary, Context<Workflow> context) {
    return Result.nonComputed(false);
  }
}
