# Resource Glue Operator

Resource Glue Operator is a powerful Kubernetes meta operator that allows you to create other operators by simply
applying a custom resource.

It provides facilities to create composed Kubernetes resources and describes how the resource
should be reconciled. Supports conditional resources in runtime, ordering of resource reconciliation, and more...

## Documentation

[Getting started](/docs/getting-started.md)

[Reference documentation](/docs/reference.md)

[Samples](/docs/samples.yaml)

[Rational and comparison with similar solutions](/docs/samples.yaml)

## Quick Introduction

### The `GlueOperator` Resource

The project introduces two Kubernetes custom resources `Glue` and `GlueOperator`.
You can use `GlueOperator` to define your own operator.
Let's take a look at an example, where we define an operator for WebPage custom resource, that represents a static website served from the Cluster. (You can see the
[full example here](https://github.com/csviri/resource-workflow-operator/blob/main/src/test/resources/sample/webpage)):

```yaml

apiVersion: "resourceglueoperator.sample/v1"
kind: WebPage
metadata:
  name: hellows
spec:
  exposed: false  # should be an ingress created or not
  html: |  # the target html
    <html>
      <head>
        <title>Hello Operator World</title>
      </head>
      <body>
        Hello World! 
      </body>
    </html>
```

To create an operator (or more precisely the controller part) with `resource-glue-operator` we have first apply
the {CRD for WebPage](https://github.com/csviri/resource-workflow-operator/blob/main/src/test/resources/sample/webpage/webpage.crd.yml).
To define how the `WebPage` should be reconciled, thus what resources should be created for
a `WebPage`, we prepare a `GlueOperator`:

```yaml
apiVersion: io.csviri.operator.resourceglue/v1beta1
kind: GlueOperator
metadata:
  name: webpage-operator
spec:
  parent:
    apiVersion: resourceglueoperator.sample/v1  # watches all the custom resource of type WebPage
    kind: WebPage
  resources:
    - name: htmlconfigmap
      resource:
        apiVersion: v1
        kind: ConfigMap
        metadata:
          name: "{parent.metadata.name}"  # the parent resource (target webpage instance) can be referenced as "parent"
        data:
          index.html: "{parent.spec.html}" # adding the html from spec to a config map
    - name: deployment
      resource:
        apiVersion: apps/v1
        kind: Deployment
        metadata:
          name: "{parent.metadata.name}"
        spec: # details omitted
          spec:
            containers:
              - name: nginx
                image: nginx:1.17.0
                volumeMounts:
                  - name: html-volume
                    mountPath: /usr/share/nginx/html
            volumes:
              - name: html-volume
                configMap:
                  name: "{parent.metadata.name}" # mounting the html using the config map to nginx server
    - name: service
      resource:
        apiVersion: v1
        kind: Service
        metadata:
          name: "{parent.metadata.name}"
        spec: # Omitted details
    - name: ingress
      condition:
        type: JSCondition
        script: | # creating just ingress only if the exposed is true (this can be changed in runtime)
          parent.spec.exposed == "true";
      resource:
        apiVersion: networking.k8s.io/v1
        kind: Ingress
        metadata:
          name: "{parent.metadata.name}"
      # Omitted Details
```

There are multiple aspects to see here. The four related resources will be templated
and applied to the cluster if such a resource is created. The reconciliation will be triggered if anything changes in the custom or managed resources. 

Note also the `condition` part for `Ingress` resource contains multiple types of conditions, `JSCondition` is
used in this example, which allows writing conditions in Javascript. The `Ingress` will be created if the `.spec.exposed` property
is true. If the property is changed to `false` after, the resource is deleted.

### The `Glue` Resource

`Glue` is very similar to `GlueOperator`, and has almost the same properties, but does not have a parent. Thus, it does not define an operator, just a set of resources to reconcile. 
Let's take a look at another example, that will show also additional features (both for `Glue` and `GlueOperator`). Typically Kubernetes does not require ordering regarding how
resources are applied, however, there are certain cases when this is needed also for Kubernetes, but especially useful when external resources are managed by Kubernetes controllers.
The following example shows how to deploy a [dynamic admission controller](https://kubernetes.io/docs/reference/access-authn-authz/extensible-admission-controllers/) that mutates 
all the `Pods`, adding annotation on them. Note that this is a tricky situation since the endpoint for the `MutatingWebhookConfiguration` is also a `Pod`, thus it should be 
first up and running before the configuration is applied, otherwise, the mutation webhook will block the changes on the pods, which would render the cluster unable to manage `Pods':

```yaml




```



