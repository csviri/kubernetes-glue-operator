# Reference Documentation

The implementation generalizes and extends [`DependentResource`](https://javaoperatorsdk.io/docs/dependent-resources) 
and [`Workflow`](https://javaoperatorsdk.io/docs/workflows) features 
of [Java Operator SDK](https://github.com/operator-framework/java-operator-sdk) and more.
Although it is limited only to Kubernetes resources it makes it very easy to use in language-independent 
(DependentResources in JOSDK are also covering external resources) way. 

## [Glue resource](https://github.com/csviri/resource-glue-operator/releases/latest/download/glues.io.csviri.operator.resourceglue-v1.yml)

`Glue` is the heart of the operator. Note that `GlueOperator` controller just creates a new `Glue` with a related resource, 
for each parent custom resource. `Glue` defines `resources` (sometimes referred to as managed resources) and `related resources`:

### Managed resources

#### Attributes

The `resources` section is a list of resources to be reconciled. It has several attributes:

- **`name`** - is a mandatory attribute. The resource is referenced by this name from other places, typically other resource templates and `JSCondition`.
  If it is used in a `JSCondition` the `name` must be a valid JavaScript variable name.
- **`resource`** - is the desired state of the resource applied by default using Server Side Apply. The resource is templated using
  [qute templating engine](https://quarkus.io/guides/qute-reference), other resources can be referenced from the templates, see below.  
  There is a restriction, that the managed resource is namespaced, and the namespace is always the same as the namespace of the `Glue`
  (and/or parent for `GlueOperator`), so the `namespace` field in resource **metadata should not be specified**.
- **`dependsOn`** - is a list of names of other managed resources (not related resources). The resource is not reconciled until all the resources
   which it depends on are not reconciled and ready (if there is a `readyPostCondition` present). 
   Note that during the cleanup phase (when a `Glue` is deleted) resources are cleaned up in reverse order.
- **`condition`** - a condition to specify if the resource should be there or not, thus even if the condition is evaluated to be `true`
   and the resource is created, if one of the following reconciliations the condition is evaluated to `false` the resource is deleted.
   (Same as `reconcilePrecondition` in Java Operator SDK)
- **`readyPostCondition`** - condition to check if the resource is considered to be ready. If a resource is ready all the resources, which depend on it
   can proceed in reconciliation.

### Related resources

### Referencing other resources

### Built-in conditions

### Reconciliation notes

The reconciliation is triggered either on a change of the `Glue` or any managed or related resources. 

On every reconciliation, each managed resource is reconciled, and if a resource is updated, it is added to a cache, so it is available for templating
for a resource that depends on it.

The `DependentResource` implementation of JOSDK makes all kinds of optimizations on the reconciliation which are utilized (or will be also here). 

## [GlueOperator resource](https://github.com/csviri/resource-glue-operator/releases/latest/download/glueoperators.io.csviri.operator.resourceglue-v1.yml)

## Deployment

Implementation is using [Quarkus Operator SDK (QOSDK)](https://github.com/quarkiverse/quarkus-operator-sdk), 
the default [configuration options](https://docs.quarkiverse.io/quarkus-operator-sdk/dev/includes/quarkus-operator-sdk.html) 
defined by QOSDK can be overridden using environment variables.

With every release, there are Kubernetes resources provided to make an initial deployment very simple.
See `kubernetes.yml` in [release assets](https://github.com/csviri/resource-glue-operator/releases).
While we will provide more options, users are encouraged to enhance/adjust this for their purposes.

Since the project is a meta-controller, it needs to have access rights to all the resources it manages. 
When creating specialized roles for a deployment, roles should contain the union of required access rights
for all the managed resource types, specifically: `["list", "watch", "create", "patch", "delete"]`
and `["list", "watch"]` for related resources.

The project is mainly tested with cluster-scoped deployment, however, QOSDK namespace-scoped deployments are also supported.

See also the upcoming deployment modes/options: [sharding with label selectors](https://github.com/csviri/resource-glue-operator/issues/50),
[watching only one custom resources type](https://github.com/csviri/resource-glue-operator/issues/54)

## Implementation details and performance

Informers are used optimally, in terms of that, for every resource type only one informer is registered in the background. Event there are more `Glue` or `GlueOperator`
resources containing the same resource type. 

The templating and some of the Javascript condition is probably the most time-consuming and resource-intensive part which will 
be continuously improved in the follow-up releases. 

## Current limitations

Note that none of the limitations are unsolvable, and will be continuously removed in the coming releases.

1. Child resources and related resources are always namespace scoped resources, and in the same namespace as the
   primary resource (`Glue` or the parent in the case of `GlueOperator`)

2. ~~Related resource changes are not triggering the reconciliation.
   Due to a bug in fabric8 client, after that is fixed, this is trivial to fix too:
   https://github.com/fabric8io/kubernetes-client/issues/5729~~

## Related documents

- [Dependent Resources documentation in Java Operator SDK](https://javaoperatorsdk.io/docs/dependent-resources)
- [Workflows documentation in Java Operator SDK](https://javaoperatorsdk.io/docs/workflows)
