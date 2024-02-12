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

The following resource defines an operator (truncated) that defines an operator:

```yaml

apiVersion: io.csviri.operator.resourceglue/v1beta1
kind: GlueOperator
metadata:
  name: webpage-operator
spec:
  parent:
    apiVersion: resourceglueoperator.sample/v1
    kind: WebPage
  resources:
    - name: html-config-map
      resource:
        apiVersion: v1
        kind: ConfigMap
        metadata:
          name: "{{parent.metadata.name}}"
        data:
          index.html: "{{{parent.spec.html}}}"
    - name: deployment
      resource:
        apiVersion: apps/v1 #
        kind: Deployment
        metadata:
          name: "{{parent.metadata.name}}"
        spec:
          selector:
            matchLabels:
              app: "{{parent.metadata.name}}"
          replicas: 1
          template:
            metadata:
              labels:
                app: "{{parent.metadata.name}}"
            spec:
              containers:
                - name: nginx
                  image: nginx:1.17.0
                  ports:
                    - containerPort: 80
                  volumeMounts:
                    - name: html-volume
                      mountPath: /usr/share/nginx/html
              volumes:
                - name: html-volume
                  configMap:
                    name: "{{parent.metadata.name}}"
    - name: service
      resource:
        apiVersion: v1
        kind: Service
        metadata:
          name: "{{parent.metadata.name}}"
        spec:
          selector:
            app: "{{parent.metadata.name}}"
          ports:
            - protocol: TCP
              port: 80
              targetPort: 80
          type: NodePort
    - name: ingress
      condition:
        type: JSCondition
        script: |
          parent.spec.exposed == "true";
      resource:
        apiVersion: networking.k8s.io/v1
        kind: Ingress
        metadata:
          name: "{{parent.metadata.name}}"
          annotations:
            nginx.ingress.kubernetes.io/rewrite-target: /$1
        spec:
          rules:
            - http:
                paths:
                  - path: /
                    pathType: Prefix
                    backend:
                      service:
                        name: "{{parent.metadata.name}}"
                        port:
                          number: 80
```