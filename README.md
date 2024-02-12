# Resource Glue Operator

Resource Glue Operator is a Kubernetes meta-operator that allows you to create operators by simply applying
a custom resource and more.

## Documentation

Find extensible documentation [here](docs/index.md)

## Quick Introduction

The project introduces two Kubernetes custom resources `Glue` and `GlueOperator`. 
You can use `Glue Operator` resource to define your own operator, by listing the resources that
will be managed, and with additional constructs, like conditions, depends-on relations and more.
Let's take a look on a simple example. You can see the [full example here](https://github.com/csviri/resource-workflow-operator/blob/main/src/test/resources/sample/webpage).

