# Spring REST Docs 与 Springdoc 对比示例

## Demo 目标

在同一套微服务中对比两种 API 文档方式：

- **Spring REST Docs**：MockMvc 测试生成片段，Asciidoctor 产出静态 HTML（`employee-service`）；
- **Springdoc OpenAPI 3**：运行时文档 + **Gateway 聚合** Swagger UI（全栈与 `master` 一致）。

已移除 **Springfox** 与 **Zuul**；栈为 **Java 25、Spring Boot 4.1.1**。

## 生成 Employee 静态文档

```shell
cd employee-service
mvn clean package
```

查看 `target/generated-snippets/`、`target/classes/static/docs/`。

## 运行完整系统

```shell
cd demos/rest-api-docs
docker compose up --build
```

- Swagger UI：<http://localhost:8060/swagger-ui.html>
- Employee 静态文档：打包后随 Employee 服务暴露在 `/docs/`（路径以生成的 HTML 文件名为准）

返回[示例总览](../../README.md)。
