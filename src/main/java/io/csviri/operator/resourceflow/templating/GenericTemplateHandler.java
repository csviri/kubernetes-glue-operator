package io.csviri.operator.resourceflow.templating;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import io.csviri.operator.resourceflow.Utils;
import io.csviri.operator.resourceflow.customresource.resourceflow.ResourceFlow;
import io.javaoperatorsdk.operator.api.reconciler.Context;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;

public class GenericTemplateHandler {
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final MustacheFactory mustacheFactory = new DefaultMustacheFactory();
  public static final String WORKFLOW_METADATA_KEY = "workflowMetadata";


  public String processTemplate(String template, ResourceFlow primary,
      Context<ResourceFlow> context) {

    // to precompile?
    var mustache = mustacheFactory.compile(new StringReader(template), "desired");

    var mustacheContext = createMustacheContextWithResources(primary, context);
    return mustache.execute(new StringWriter(), mustacheContext).toString();
  }

  private static Map<String, Map> createMustacheContextWithResources(ResourceFlow primary,
      Context<ResourceFlow> context) {
    var actualResourcesByName = Utils.getActualResourcesByNameInWorkflow(context, primary);

    Map<String, Map> mustacheContext = new HashMap<>();

    actualResourcesByName.entrySet().stream().forEach(e -> mustacheContext.put(e.getKey(),
        e.getValue() == null ? null : objectMapper.convertValue(e.getValue(), Map.class)));

    mustacheContext.put(WORKFLOW_METADATA_KEY,
        objectMapper.convertValue(primary.getMetadata(), Map.class));

    return mustacheContext;
  }
}
