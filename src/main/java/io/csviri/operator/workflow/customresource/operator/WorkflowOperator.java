package io.csviri.operator.workflow.customresource.operator;

import io.csviri.operator.workflow.customresource.workflow.WorkflowSpec;
import io.csviri.operator.workflow.customresource.workflow.WorkflowStatus;
import io.fabric8.kubernetes.client.CustomResource;

public class WorkflowOperator extends CustomResource<WorkflowSpec, WorkflowStatus> {
}
