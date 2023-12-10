package io.csviri.operator.workflow;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.csviri.operator.workflow.customresource.operator.WorkflowOperator;
import io.csviri.operator.workflow.customresource.operator.WorkflowOperatorSpec;
import io.csviri.operator.workflow.customresource.workflow.Workflow;
import io.csviri.operator.workflow.customresource.workflow.WorkflowSpec;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.javaoperatorsdk.operator.api.config.informer.InformerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.processing.GroupVersionKind;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;

@ControllerConfiguration
public class WorkflowOperatorReconciler
    implements Reconciler<WorkflowOperator>, EventSourceInitializer<WorkflowOperator> {

  public static final String WORKFLOW_LABEL_KEY = "foroperator";
  public static final String WORKFLOW_LABEL_VALUE = "true";
  public static final String WATCH_PREFIX = "workflow-operator-watch-cr/";

  public static final String WATCH_GROUP = WATCH_PREFIX + "group";
  public static final String WATCH_VERSION = WATCH_PREFIX + "version";
  public static final String WATCH_KIND = WATCH_PREFIX + "kind";
  public static final String WATCH_NAME = WATCH_PREFIX + "name";
  public static final String WATCH_NAMESPACE = WATCH_PREFIX + "namespace";

  private InformerEventSource<Workflow, WorkflowOperator> workflowEventSource;

  // todo customizable
  public final static String WORKFLOW_TARGET_NAMESPACE = "default";

  @Override
  public UpdateControl<WorkflowOperator> reconcile(WorkflowOperator workflowOperator,
      Context<WorkflowOperator> context) {

    var targetCREventSource = getOrRegisterEventSource(workflowOperator, context);
    targetCREventSource.list().forEach(cr -> {
      var workFlow = workflowEventSource
          .get(new ResourceID(cr.getMetadata().getName(), cr.getMetadata().getNamespace()));
      // todo match / update
      if (workFlow.isEmpty()) {
        context.getClient().resource(createWorkflow(cr, workflowOperator)).create();
      }
    });

    return UpdateControl.noUpdate();
  }

  // todo cluster scope support
  private Workflow createWorkflow(GenericKubernetesResource cr, WorkflowOperator workflowOperator) {
    var res = new Workflow();
    Map<String, String> annotation = new HashMap<>();
    GroupVersionKind gvk = new GroupVersionKind(cr.getApiVersion(), cr.getKind());
    annotation.put(WATCH_GROUP, gvk.getGroup());
    annotation.put(WATCH_VERSION, gvk.getVersion());
    annotation.put(WATCH_KIND, gvk.getKind());
    annotation.put(WATCH_NAME, cr.getMetadata().getName());
    annotation.put(WATCH_NAMESPACE, cr.getMetadata().getNamespace());



    res.setMetadata(new ObjectMetaBuilder()
        // todo proper naming based on resource name
        .withAnnotations(annotation)
        .withName(cr.getMetadata().getName())
        // these workflows are either in a static namespace or in the same namespace as CR-s, or
        // same as WO
        // for now for sake of simplicity is static will, eventually best would be to have it in
        // same as WO
        .withNamespace(cr.getMetadata().getNamespace())
        // .withLabels(Map.of(WORKFLOW_LABEL_KEY, WORKFLOW_LABEL_VALUE))
        .build());
    res.setSpec(toWorkflowSpec(workflowOperator.getSpec()));
    res.addOwnerReference(workflowOperator);
    return res;
  }

  private WorkflowSpec toWorkflowSpec(WorkflowOperatorSpec spec) {
    var res = new WorkflowSpec();
    res.setResources(spec.getResources());
    return res;
  }

  private InformerEventSource<GenericKubernetesResource, WorkflowOperator> getOrRegisterEventSource(
      WorkflowOperator workflowOperator, Context<WorkflowOperator> context) {
    var spec = workflowOperator.getSpec();
    var gvk = new GroupVersionKind(spec.getGroup(), spec.getVersion(), spec.getKind());
    InformerEventSource<GenericKubernetesResource, WorkflowOperator> es;
    // note that this allows just one operator per gvk (what is limitation but ok for now)
    try {
      es = (InformerEventSource<GenericKubernetesResource, WorkflowOperator>) context
          .eventSourceRetriever()
          .getResourceEventSourceFor(GenericKubernetesResource.class, gvk.toString());
    } catch (IllegalArgumentException e) {
      es = new InformerEventSource<>(InformerConfiguration.from(gvk,
          context.eventSourceRetriever().eventSourceContexForDynamicRegistration())
          .withSecondaryToPrimaryMapper(
              resource -> Set.of(ResourceID.fromResource(workflowOperator)))
          .build(), context.eventSourceRetriever().eventSourceContexForDynamicRegistration());
      context.eventSourceRetriever().dynamicallyRegisterEventSource(gvk.toString(), es);
    }
    return es;
  }

  @Override
  public Map<String, EventSource> prepareEventSources(
      EventSourceContext<WorkflowOperator> eventSourceContext) {
    workflowEventSource = new InformerEventSource<>(
        InformerConfiguration.from(Workflow.class, eventSourceContext)
            // .withLabelSelector(WORKFLOW_LABEL_KEY + "=" + WORKFLOW_LABEL_VALUE)
            .build(),
        eventSourceContext);
    return EventSourceInitializer.nameEventSources(workflowEventSource);
  }
}
