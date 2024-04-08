# Reference Documentation

The implementation generalizes and extends [`DependentResource`](https://javaoperatorsdk.io/docs/dependent-resources) 
and [`Workflow`](https://javaoperatorsdk.io/docs/workflows) features 
of [Java Operator SDK](https://github.com/operator-framework/java-operator-sdk) and more.
Although it is limited only to Kubernetes resources it makes it very easy to use in language-independent 
(DependentResources in JOSDK are also covering external resources) way. 

## [Glue resource](https://github.com/csviri/resource-glue-operator/releases/latest/download/glues.io.csviri.operator.resourceglue-v1.yml)

`Glue` is the hearth of the operator. Note that `GlueOperator` and related controller just creates a new `Glue` with a target related resource (see below), 
for each parent custom resource. `Glue` defines `resources` (sometimes refered as managed resources) and `related resources`.

### Managed resources

### Related Resources


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
be continuously improved in the followup releases. 

## Current Limitations

Note that none of the limitations are unsolvable, and will be continuously removed in the coming releases.

1. Child resources and related resources are always namespace scoped resources, and in the same namespace as the
   primary resource (`Glue` or the parent in the case of `GlueOperator`)

2. ~~Related resource changes are not triggering the reconciliation.
   Due to a bug in fabric8 client, after that is fixed, this is trivial to fix too:
   https://github.com/fabric8io/kubernetes-client/issues/5729~~

## Related Documents

- [Dependent Resources documentation in Java Operator SDK](https://javaoperatorsdk.io/docs/dependent-resources)
- [Workflows documentation in Java Operator SDK](https://javaoperatorsdk.io/docs/workflows)
