package io.csviri.operator.workflow;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.csviri.operator.workflow.conditions.JavaScripCondition;
import io.csviri.operator.workflow.customresource.workflow.DependentResourceSpec;
import io.csviri.operator.workflow.customresource.workflow.Workflow;
import io.csviri.operator.workflow.customresource.workflow.WorkflowSpec;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JavaScripConditionTest {

  Context<Workflow> mockContext = mock(Context.class);
  DependentResource<GenericKubernetesResource, Workflow> dr = mock(DependentResource.class);

  @Test
  void javaScriptSimpleConditionTest() {

    var condition = new JavaScripCondition("""
        x = 1;
        x<2;
        """);

    when(mockContext.getSecondaryResources(any())).thenReturn(Set.of());
    when(dr.getSecondaryResource(any(), any())).thenReturn(Optional.of(configMap()));

    var res = condition.isMet(dr, null, mockContext);
    assertThat(res).isTrue();
  }

  @Test
    void injectsTargetResourceResource() {
        when(mockContext.getSecondaryResources(any())).thenReturn(Set.of());
        when(dr.getSecondaryResource(any(), any())).thenReturn(Optional.of(configMap()));

        var condition = new JavaScripCondition("""
                    target.data.key1 == "val1";
                """);

        var res = condition.isMet(dr, null, mockContext);
        assertThat(res).isTrue();
    }

  @Test
    void injectsSecondaryResourcesResource() {
        when(mockContext.getSecondaryResources(any())).thenReturn(Set.of(configMap()));
        when(dr.getSecondaryResource(any(), any())).thenReturn(Optional.of(configMap()));

        Workflow workflow = new Workflow();
        workflow.setSpec(new WorkflowSpec());
        workflow.getSpec().setResources(new ArrayList<>());
        var drSpec = new DependentResourceSpec();
        drSpec.setName("secondary");
        drSpec.setResource(configMap());
        workflow.getSpec().getResources().add(drSpec);

        var condition = new JavaScripCondition("""
                    secondary.data.key1 == "val1";
                """);

        var res = condition.isMet(dr, workflow, mockContext);
        assertThat(res).isTrue();
    }

  private GenericKubernetesResource configMap() {
    try (InputStream is = JavaScripConditionTest.class.getResourceAsStream("/ConfigMap.yaml")) {
      return Serialization.unmarshal(is, GenericKubernetesResource.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
