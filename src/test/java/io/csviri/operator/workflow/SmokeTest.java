package io.csviri.operator.workflow;

import io.csviri.operator.workflow.customresource.DependentResourceSpec;
import io.csviri.operator.workflow.customresource.WorkflowCustomResource;
import io.csviri.operator.workflow.customresource.WorkflowSpec;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.junit.LocallyRunOperatorExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class SmokeTest {

    @RegisterExtension
    LocallyRunOperatorExtension extension =
            LocallyRunOperatorExtension.builder().withReconciler(new WorkflowReconciler())
                    .build();

    @Test
    void smokeTest() {
        extension.create(testWorkflow());

        await().untilAsserted(()-> {
            var cm = extension.get(ConfigMap.class,"test1");
            assertThat(cm).isNotNull();
        });
    }

    WorkflowCustomResource testWorkflow() {
        var resource = new WorkflowCustomResource();
        resource.setMetadata(new ObjectMetaBuilder()
                .withName("test1")
                .build());
        var spec = new WorkflowSpec();
        resource.setSpec(spec);
        spec.setResources(List.of(dependentSpec()));
        return resource;
    }

    private DependentResourceSpec dependentSpec() {
        try (InputStream is = SmokeTest.class.getResourceAsStream("/ConfigMap.yaml")) {
        var spec = new DependentResourceSpec();

            var cm = Serialization.unmarshal(is, GenericKubernetesResource.class);
            spec.setResource(cm);
            return spec;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
