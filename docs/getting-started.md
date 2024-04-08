# Getting Started

## Start Minikube/Kind

Start a local Kubernetes cluster using for example [Minikube](https://minikube.sigs.k8s.io/docs/start/) or [Kind](https://kind.sigs.k8s.io/docs/user/quick-start/#installation).

## Deploy the Operator

Apply the CustomResourceDefinitions:

```bash
kubectl apply -f https://github.com/csviri/resource-glue-operator/releases/latest/download/glues.io.csviri.operator.resourceglue-v1.yml -f https://github.com/csviri/resource-glue-operator/releases/latest/download/glueoperators.io.csviri.operator.resourceglue-v1.yml
```

Deploy the `resource-glue-operator`:

```bash
kubectl apply -f https://github.com/csviri/resource-glue-operator/releases/latest/download/kubernetes.yml
```

Note that this deployment give the controller access to all the resource on the cluster, this not what you might want in production.

## Try out the WebPage Sample

First apply the CustomResourceDefinition for `WebPage` :

```bash
kubectl apply -f https://raw.githubusercontent.com/csviri/resource-glue-operator/main/src/test/resources/sample/webpage/webpage.crd.yaml
```

Apply the `GlueOperator` for `WebPage`:

```bash
kubectl apply -f https://raw.githubusercontent.com/csviri/resource-glue-operator/main/src/test/resources/sample/webpage/webpage.operator.yaml
```

Create a new static WebPage:

```bash
kubectl apply -f https://raw.githubusercontent.com/csviri/resource-glue-operator/main/src/test/resources/sample/webpage/webpage.sample.yaml
```

All done! Check the resources created, if using minikube you can open the served WebPage with `minikube service webpage1`.

