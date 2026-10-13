# Kubernetes 微服务部署示例

## Demo 目标

将 Employee、Department、Organization 与 **Spring Cloud Gateway** 部署到 Kubernetes：

- Deployment / Service 提供稳定 DNS（`employee`、`department`、`organization`）；
- **Ingress** 使用 `networking.k8s.io/v1`；
- Gateway 通过集群内服务名转发（不再使用 Zuul / Ribbon / Sleuth）；
- 业务数据仍使用 MongoDB。

应用构建：**Java 25、Spring Boot 4.1.1**；API 文档使用 **Springdoc**。

## 部署

```shell
kubectl apply -f kubernetes/mongodb-deployment.yaml
kubectl apply -f kubernetes/employee-deployment.yaml
kubectl apply -f kubernetes/department-deployment.yaml
kubectl apply -f kubernetes/organization-deployment.yaml
kubectl apply -f kubernetes/gateway-deployment.yaml
kubectl apply -f kubernetes/ingress.yaml
```

Gateway 路由配置见 `gateway-service/src/main/resources/application.yml`（`http://employee:8080` 等）。

## 已知限制

- 清单中的 `piomin/*:1.0` 镜像需自行构建并替换；
- Ingress 注解与 `ingressClassName` 需与集群 Ingress Controller 匹配。

返回[示例总览](../../README.md)。
