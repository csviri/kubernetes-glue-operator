# Getting Started

## Start Minikube/Kind

Start a local Kubernetes cluster using for example [Minikube](https://minikube.sigs.k8s.io/docs/start/) or [Kind](https://kind.sigs.k8s.io/docs/user/quick-start/#installation).

## Deploy the Operator

Apply custom resource definitions:

```bash
kubectl apply -f https://github.com/csviri/resource-glue-operator/releases/latest/download/glues.io.csviri.operator.resourceglue-v1.yml -f https://github.com/csviri/resource-glue-operator/releases/latest/download/glueoperators.io.csviri.operator.resourceglue-v1.yml
```

Deploy the `resource-glue-operator`:

```bash
kubectl apply -f https://github.com/csviri/resource-glue-operator/releases/latest/download/kubernetes.yml
```

## Try out the WebPage Sample


