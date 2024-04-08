# Reference Documentation

## Deployment

Implementation is using [Quarkus Operator SDK](https://github.com/quarkiverse/quarkus-operator-sdk), 
the default [configuration options](https://docs.quarkiverse.io/quarkus-operator-sdk/dev/includes/quarkus-operator-sdk.html) 
defined by QOSDK can be overridden using environment variables.

### Deployment Modes



## Glue Resource

## GlueOperator Resource



# Current Limitations

Note that none of the limitations are unsolvable, and will be continuously removed in the coming releases.

1. Child resources and related resources are always namespace scoped resources, and in the same namespace as the
   primary resource (`Glue` or the parent in case of `GlueOperator`)

2. ~~Related resource changes are not triggering the reconciliation.
   Due to a bug in fabric8 client, after that is fixed, this is trivial to fix too:
   https://github.com/fabric8io/kubernetes-client/issues/5729~~

## Related Documents

- [Dependent Resources documentation in Java Operator SDK](https://javaoperatorsdk.io/docs/dependent-resources)
- [Workflows documentation in Java Operator SDK](https://javaoperatorsdk.io/docs/workflows)
