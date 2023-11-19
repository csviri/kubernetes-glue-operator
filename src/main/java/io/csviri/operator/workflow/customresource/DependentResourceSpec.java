package io.csviri.operator.workflow.customresource;

import io.fabric8.crd.generator.annotation.PreserveUnknownFields;
import io.fabric8.kubernetes.api.model.HasMetadata;


public class DependentResourceSpec {

    private String name;

    // templating, eventually with QUTE
//  private String resourceTemplate;

//  alternative resource definition
    @PreserveUnknownFields
    private HasMetadata resource;

//    private List<String> dependsOn;

    // the reconcile condition in JOSDK
//    private Condition condition;

    public String getName() {
        return name;
    }

    public DependentResourceSpec setName(String name) {
        this.name = name;
        return this;
    }
//
//    public List<String> getDependsOn() {
//        return dependsOn;
//    }
//
//    public DependentResourceSpec setDependsOn(List<String> dependsOn) {
//        this.dependsOn = dependsOn;
//        return this;
//    }
//
//    public Condition getCondition() {
//        return condition;
//    }
//
//    public DependentResourceSpec setCondition(Condition condition) {
//        this.condition = condition;
//        return this;
//    }

//    public DependentResourceSpec setCondition(Condition condition) {
//        this.condition = condition;
//        return this;
//    }

    public HasMetadata getResource() {
        return resource;
    }

    public DependentResourceSpec setResource(HasMetadata resource) {
        this.resource = resource;
        return this;
    }

    @Override
    public String toString() {
        return "DependentResourceSpec{" +
                "name='" + name + '\'' +
                ", resource=" + resource +
                '}';
    }
}
