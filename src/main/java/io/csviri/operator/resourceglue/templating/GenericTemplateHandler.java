package io.csviri.operator.resourceglue.templating;

import java.util.HashMap;
import java.util.Map;

import io.csviri.operator.resourceglue.Utils;
import io.csviri.operator.resourceglue.customresource.glue.Glue;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;

import com.fasterxml.jackson.databind.ObjectMapper;

public class GenericTemplateHandler {

  public static final String WORKFLOW_METADATA_KEY = "glueMetadata";

  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final Engine engine = Engine.builder().addDefaults().build();

  public String processTemplate(String template, Glue primary,
      Context<Glue> context) {

    Template hello = engine.parse(template);
    var data = createDataWithResources(primary, context);
    return hello.data(data).render();
  }

  @SuppressWarnings("rawtypes")
  private static Map<String, Map> createDataWithResources(Glue primary,
      Context<Glue> context) {
    Map<String, Map> res = new HashMap<>();
    var actualResourcesByName = Utils.getActualResourcesByNameInWorkflow(context, primary);

    actualResourcesByName.forEach((key, value) -> res.put(key,
        value == null ? null : objectMapper.convertValue(value, Map.class)));

    res.put(WORKFLOW_METADATA_KEY,
        objectMapper.convertValue(primary.getMetadata(), Map.class));

    return res;
  }
}
