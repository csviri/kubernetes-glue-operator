package io.csviri.operator.resourceglue.dependent;

import io.csviri.operator.resourceglue.Utils;
import io.csviri.operator.resourceglue.customresource.glue.Glue;
import io.csviri.operator.resourceglue.reconciler.glue.GlueReconciler;
import io.csviri.operator.resourceglue.templating.GenericTemplateHandler;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Deleter;
import io.javaoperatorsdk.operator.processing.GroupVersionKind;
import io.javaoperatorsdk.operator.processing.dependent.Creator;
import io.javaoperatorsdk.operator.processing.dependent.Updater;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.GenericKubernetesDependentResource;

public class GenericDependentResource
    extends GenericKubernetesDependentResource<Glue>
    implements Deleter<Glue>,
    Updater<GenericKubernetesResource, Glue>,
    Creator<GenericKubernetesResource, Glue> {

  private final GenericKubernetesResource desired;
  private final String desiredTemplate;
  private final String name;
  // todo share between instances
  private final GenericTemplateHandler genericTemplateHandler = new GenericTemplateHandler();

  public GenericDependentResource(GenericKubernetesResource desired, String name) {
    super(new GroupVersionKind(desired.getApiVersion(), desired.getKind()));
    this.desired = desired;
    this.desiredTemplate = null;
    this.name = name;
  }

  public GenericDependentResource(String desiredTemplate, String name) {
    super(new GroupVersionKind(Utils.getApiVersionFromTemplate(desiredTemplate),
        Utils.getKindFromTemplate(desiredTemplate)));
    this.name = name;
    this.desiredTemplate = desiredTemplate;
    this.desired = null;
  }

  @Override
  protected GenericKubernetesResource desired(Glue primary,
      Context<Glue> context) {

    var template = desired == null ? desiredTemplate : Serialization.asYaml(desired);

    var res = genericTemplateHandler.processTemplate(template, primary, context);
    var resultDesired = Serialization.unmarshal(res, GenericKubernetesResource.class);

    resultDesired.getMetadata().getAnnotations()
        .put(GlueReconciler.DEPENDENT_NAME_ANNOTATION_KEY, name);

    // set only for cluster scoped when detection is ready
    if (resultDesired.getMetadata().getNamespace() == null) {
      resultDesired.getMetadata().setNamespace(primary.getMetadata().getNamespace());
    }
    return resultDesired;
  }

  @Override
  public Result<GenericKubernetesResource> match(GenericKubernetesResource actualResource,
      Glue primary, Context<Glue> context) {
    // todo create issue in JOSDK and link here
    if (actualResource.getKind().equals("Deployment") && actualResource.getApiVersion().equals("apps/v1")) {
      return super.match(actualResource,primary,context);
    }
    return Result.nonComputed(false);
  }
}
