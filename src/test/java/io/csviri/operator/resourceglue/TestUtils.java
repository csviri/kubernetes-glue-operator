package io.csviri.operator.resourceglue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.csviri.operator.resourceglue.customresource.glue.Glue;
import io.csviri.operator.resourceglue.customresource.operator.GlueOperator;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonDeletingOperation;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.ReconcilerUtils;

public class TestUtils {

  public static final int CRD_READY_WAIT = 1000;

  private static final Logger log = LoggerFactory.getLogger(TestUtils.class);

  public static Glue loadResoureFlow(String path) {
    try (InputStream is = TestUtils.class.getResourceAsStream(path)) {
      return Serialization.unmarshal(is, Glue.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static GlueOperator loadResourceFlowOperator(String path) {
    try (InputStream is = TestUtils.class.getResourceAsStream(path)) {
      return Serialization.unmarshal(is, GlueOperator.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static GenericKubernetesResource load(String path) {
    try (InputStream is = TestUtils.class.getResourceAsStream(path)) {
      return Serialization.unmarshal(is, GenericKubernetesResource.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static GenericKubernetesResource createOrUpdate(KubernetesClient client, String path) {
    return client.resource(load(path)).createOr(NonDeletingOperation::update);
  }

  public static void applyCrd(Class<? extends HasMetadata> resourceClass, KubernetesClient client) {
    applyCrd(ReconcilerUtils.getResourceTypeName(resourceClass), client);
  }

  public static void applyCrd(String resourceTypeName, KubernetesClient client) {
    String path = "/META-INF/fabric8/" + resourceTypeName + "-v1.yml";
    try (InputStream is = TestUtils.class.getResourceAsStream(path)) {
      if (is == null) {
        throw new IllegalStateException("Cannot find CRD at " + path);
      }
      var crdString = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      log.debug("Applying CRD: {}", crdString);
      final var crd = client.load(new ByteArrayInputStream(crdString.getBytes()));
      crd.createOrReplace();
      Thread.sleep(CRD_READY_WAIT); // readiness is not applicable for CRD, just wait a little
      log.debug("Applied CRD with path: {}", path);
    } catch (InterruptedException ex) {
      log.error("Interrupted.", ex);
      Thread.currentThread().interrupt();
    } catch (Exception ex) {
      throw new IllegalStateException("Cannot apply CRD yaml: " + path, ex);
    }
  }

}
