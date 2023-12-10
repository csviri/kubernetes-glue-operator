package io.csviri.operator.workflow.dependent;

import io.csviri.operator.workflow.customresource.workflow.Workflow;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.GarbageCollected;
import io.javaoperatorsdk.operator.processing.GroupVersionKind;
import io.javaoperatorsdk.operator.processing.dependent.Creator;
import io.javaoperatorsdk.operator.processing.dependent.Updater;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.GenericKubernetesDependentResource;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;

public class GenericDependentResource
    extends GenericKubernetesDependentResource<Workflow> implements GarbageCollected<Workflow>,
    Updater<GenericKubernetesResource, Workflow>,
    Creator<GenericKubernetesResource, Workflow> {

  private GenericKubernetesResource desired;
  private MustacheFactory mustacheFactory = new DefaultMustacheFactory();

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

    // this can be precompiled
    // var mustache = mustacheFactory.compile(Serialization.asYaml(desired));
    // Map<String, Object> mustacheContext = new HashMap<>(); // add all relevant resources
    // var res = mustache.execute(new StringWriter(),mustacheContext);
    // var resultDesired = Serialization.unmarshal(res.toString(),GenericKubernetesResource.class);
    // return resultDesired;

    return desired;
  }

  @Override
  public Result<GenericKubernetesResource> match(GenericKubernetesResource actualResource,
      Workflow primary, Context<Workflow> context) {
    return Result.nonComputed(false);
  }
}
