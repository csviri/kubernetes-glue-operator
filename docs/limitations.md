
# Current Limitations

Note that none of the limitations are unsolvable, and will be continuously removed in the coming releases.

1. Child resources and related resources are always namespace scoped resources, and in the same namespace as the 
   primary resource (`Glue` or the parent in case of `GlueOperator`)
2. ~~Related resource changes are not triggering the reconciliation.
   Due to a bug in fabric8 client, after that is fixed, this is trivial to fix too:
   https://github.com/fabric8io/kubernetes-client/issues/5729~~

