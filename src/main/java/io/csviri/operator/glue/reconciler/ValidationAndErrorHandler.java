package io.csviri.operator.glue.reconciler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.csviri.operator.glue.GlueException;
import io.csviri.operator.glue.customresource.AbstractStatus;
import io.csviri.operator.glue.customresource.glue.DependentResourceSpec;
import io.csviri.operator.glue.customresource.glue.GlueSpec;
import io.csviri.operator.glue.customresource.glue.RelatedResourceSpec;
import io.fabric8.kubernetes.client.CustomResource;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusUpdateControl;

import jakarta.inject.Singleton;

@Singleton
public class ValidationAndErrorHandler {
  private static final Logger log = LoggerFactory.getLogger(ValidationAndErrorHandler.class);

  public <T extends CustomResource<?, ? extends AbstractStatus>> ErrorStatusUpdateControl<T> updateStatusErrorMessage(
      Exception e,
      T resource) {
    log.error("Error during reconciliation of resource. Name: {} namespace: {}, Kind: {}",
        resource.getMetadata().getName(), resource.getMetadata().getNamespace(), resource.getKind(),
        e);
    if (e instanceof ValidationAndErrorHandler.NonUniqueNameException ex) {
      resource.getStatus()
          .setErrorMessage("Non unique names found: " + String.join(",", ex.getDuplicates()));
    } else {
      resource.getStatus().setErrorMessage("Error during reconciliation");
    }
    return ErrorStatusUpdateControl.updateStatus(resource);
  }

  public void checkIfNamesAreUnique(GlueSpec glueSpec) {
    Set<String> seen = new HashSet<>();
    List<String> duplicates = new ArrayList<>();

    Consumer<String> deduplicate = n -> {
      if (seen.contains(n)) {
        duplicates.add(n);
      } else {
        seen.add(n);
      }
    };
    glueSpec.getResources().stream().map(DependentResourceSpec::getName).forEach(deduplicate);
    glueSpec.getRelatedResources().stream().map(RelatedResourceSpec::getName).forEach(deduplicate);

    if (!duplicates.isEmpty()) {
      throw new NonUniqueNameException(duplicates);
    }

  }

  public static class NonUniqueNameException extends GlueException {

    private final List<String> duplicates;

    public NonUniqueNameException(List<String> duplicates) {
      this.duplicates = duplicates;
    }

    public List<String> getDuplicates() {
      return duplicates;
    }
  }

}
