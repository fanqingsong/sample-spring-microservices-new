# GraphQL 微服务示例（Spring for GraphQL）

## Demo 目标

- Employee、Department 通过 **Spring for GraphQL** 暴露 `/graphql`；
- Department 使用 **HttpGraphQlClient** + **LoadBalancer** 调用 Employee（替代 Apollo + 手写 Eureka URL）；
- Organization 仍保留 **REST + OpenFeign**，演示 GraphQL 与 REST 混合架构。

技术栈：**Java 25、Spring Boot 4.1.1、Spring Cloud 2025.1.3**。

## 启动顺序

```shell
cd discovery-service && mvn spring-boot:run
cd employee-service && mvn spring-boot:run
cd department-service && mvn spring-boot:run
cd organization-service && mvn spring-boot:run   # 可选
```

端口：Eureka `8061`，Employee GraphQL `8090`，Department GraphQL `8091`，Organization REST `8082`。

## 验证

```shell
curl -X POST http://localhost:8090/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"{ employees { id name position } }"}'
```

GraphiQL（若启用）：<http://localhost:8090/graphiql>

## 阅读顺序

1. `employee-service/src/main/resources/graphql/employee.graphqls`
2. `EmployeeGraphController`、`DepartmentGraphController`
3. `EmployeeGraphClient`（跨服务 GraphQL 调用）

返回[示例总览](../../README.md)。
