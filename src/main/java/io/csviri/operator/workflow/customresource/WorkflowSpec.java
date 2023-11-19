package io.csviri.operator.workflow.customresource;

import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;

import java.util.List;

public class WorkflowSpec {

    private List<DependentResourceSpec> resources;

    public List<DependentResourceSpec> getResources() {
        return resources;
    }

    public WorkflowSpec setResources(List<DependentResourceSpec> resources) {
        this.resources = resources;
        return this;
    }
}
