package io.csviri.operator.workflow.reconciler;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.csviri.operator.workflow.customresource.operator.WorkflowOperator;
import io.csviri.operator.workflow.customresource.operator.WorkflowOperatorSpec;
import io.csviri.operator.workflow.customresource.workflow.Workflow;
import io.csviri.operator.workflow.customresource.workflow.WorkflowSpec;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.javaoperatorsdk.operator.api.config.informer.InformerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.processing.GroupVersionKind;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;

@ControllerConfiguration
public class WorkflowOperatorReconciler
    implements Reconciler<WorkflowOperator>, EventSourceInitializer<WorkflowOperator>,
    Cleaner<WorkflowOperator> {

  private static final Logger log = LoggerFactory.getLogger(WorkflowOperatorReconciler.class);

  public static final String WORKFLOW_LABEL_KEY = "foroperator";
  public static final String WORKFLOW_LABEL_VALUE = "true";
  public static final String WATCH_PREFIX = "workflow-operator-watch-cr/";

  public static final String WATCH_GROUP = WATCH_PREFIX + "group";
  public static final String WATCH_VERSION = WATCH_PREFIX + "version";
  public static final String WATCH_KIND = WATCH_PREFIX + "kind";
  public static final String WATCH_NAME = WATCH_PREFIX + "name";

  private InformerEventSource<Workflow, WorkflowOperator> workflowEventSource;

  @Override
  public UpdateControl<WorkflowOperator> reconcile(WorkflowOperator workflowOperator,
      Context<WorkflowOperator> context) {

    log.debug("Reconciling: {} in namespace: {}", workflowOperator.getMetadata().getName(),
        workflowOperator.getMetadata().getNamespace());

    var targetCREventSource = getOrRegisterCustomResourceEventSource(workflowOperator, context);
    targetCREventSource.list().forEach(cr -> {
      var workFlow = workflowEventSource
          .get(new ResourceID(workflowName(cr), cr.getMetadata().getNamespace()));
      var targetWorkflow = createWorkflow(cr, workflowOperator);
      if (workFlow.isEmpty()) {
        context.getClient().resource(targetWorkflow).create();
      } else if (!match(workFlow.orElseThrow(), targetWorkflow)) {
        context.getClient().resource(targetWorkflow).update();
      }
    });

    return UpdateControl.noUpdate();
  }

  private boolean match(Workflow actualWorkflow, Workflow targetWorkflow) {
    // for now cannot change watched resource, coming with related resources
    return actualWorkflow.getSpec().equals(targetWorkflow.getSpec());
  }

  private Workflow createWorkflow(GenericKubernetesResource cr, WorkflowOperator workflowOperator) {
    var res = new Workflow();
    Map<String, String> annotation = new HashMap<>();
    GroupVersionKind gvk = new GroupVersionKind(cr.getApiVersion(), cr.getKind());
    annotation.put(WATCH_GROUP, gvk.getGroup());
    annotation.put(WATCH_VERSION, gvk.getVersion());
    annotation.put(WATCH_KIND, gvk.getKind());
    annotation.put(WATCH_NAME, cr.getMetadata().getName());

    res.setMetadata(new ObjectMetaBuilder()
        .withAnnotations(annotation)
        .withName(workflowName(cr))
        .withNamespace(cr.getMetadata().getNamespace())
        .withLabels(Map.of(WORKFLOW_LABEL_KEY, WORKFLOW_LABEL_VALUE))
        .build());
    res.setSpec(toWorkflowSpec(workflowOperator.getSpec()));
    res.addOwnerReference(cr);
    return res;
  }

  private WorkflowSpec toWorkflowSpec(WorkflowOperatorSpec spec) {
    var res = new WorkflowSpec();
    res.setResources(spec.getResources());
    return res;
  }

  private InformerEventSource<GenericKubernetesResource, WorkflowOperator> getOrRegisterCustomResourceEventSource(
      WorkflowOperator workflowOperator, Context<WorkflowOperator> context) {
    var spec = workflowOperator.getSpec();
    var gvk = new GroupVersionKind(spec.getParent().getApiVersion(), spec.getParent().getKind());
    InformerEventSource<GenericKubernetesResource, WorkflowOperator> es;
    // note that this allows just one operator per gvk (what is limitation but ok for now)
    try {
      es = (InformerEventSource<GenericKubernetesResource, WorkflowOperator>) context
          .eventSourceRetriever()
          .getResourceEventSourceFor(GenericKubernetesResource.class, gvk.toString());
      es.start();
    } catch (IllegalArgumentException e) {
      es = new InformerEventSource<>(InformerConfiguration.from(gvk,
          context.eventSourceRetriever().eventSourceContextForDynamicRegistration())
          .withSecondaryToPrimaryMapper(
              resource -> Set.of(ResourceID.fromResource(workflowOperator)))
          .build(), context.eventSourceRetriever().eventSourceContextForDynamicRegistration());
      context.eventSourceRetriever().dynamicallyRegisterEventSource(gvk.toString(), es);
    }
    return es;
  }

  @Override
  public Map<String, EventSource> prepareEventSources(
      EventSourceContext<WorkflowOperator> eventSourceContext) {
    workflowEventSource = new InformerEventSource<>(
        InformerConfiguration.from(Workflow.class, eventSourceContext)
            .withLabelSelector(WORKFLOW_LABEL_KEY + "=" + WORKFLOW_LABEL_VALUE)
            .build(),
        eventSourceContext);
    return EventSourceInitializer.nameEventSources(workflowEventSource);
  }

  @Override
  public DeleteControl cleanup(WorkflowOperator workflowOperator,
      Context<WorkflowOperator> context) {
    var spec = workflowOperator.getSpec();
    var gvk = new GroupVersionKind(spec.getParent().getApiVersion(), spec.getParent().getKind());
    context.eventSourceRetriever().dynamicallyDeRegisterEventSource(gvk.toString());
    return DeleteControl.defaultDelete();
  }

  private String workflowName(GenericKubernetesResource cr) {
    return KubernetesResourceUtil.sanitizeName(cr.getMetadata().getName() + "-" + cr.getKind());
  }

}
