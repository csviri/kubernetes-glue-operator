package io.csviri.operator.workflow;

import java.util.HashMap;
import java.util.Map;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.csviri.operator.workflow.WorkflowOperatorCustomResource;

@KubernetesDependent
public class ConfigMapDependentResource
        extends CRUDKubernetesDependentResource<ConfigMap, WorkflowOperatorCustomResource> {

    public ConfigMapDependentResource() {
        super(ConfigMap.class);
    }

    @Override
    protected ConfigMap desired(WorkflowOperatorCustomResource primary,
                                Context<WorkflowOperatorCustomResource> context) {
        return new ConfigMapBuilder()
                .withMetadata(
                        new ObjectMetaBuilder()
                                .withName(primary.getMetadata().getName())
                                .withNamespace(primary.getMetadata().getNamespace())
                                .build())
                .withData(Map.of("data", primary.getSpec().getValue()))
                .build();
    }
}