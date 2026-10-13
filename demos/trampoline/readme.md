# 使用 Trampoline 管理本地微服务

## Demo 目标

在本地同时运行 Config、Eureka、三个业务服务与 **Spring Cloud Gateway**，可选配合 [Trampoline](http://ernestort.github.io/Trampoline/) 批量启停与看日志；也支持 `docker compose up --build`（与 `master` 相同编排）。

本目录代码已对齐 **Java 25 / Spring Boot 4.1.1**，已移除 **Zuul `proxy-service`**，统一使用 Gateway 作为入口。

## 负载均衡实验

`config-service/src/main/resources/config/employee-service-instance2.yml` 将第二个 Employee 实例端口设为 **9090**。启动：

```shell
cd employee-service
mvn spring-boot:run -Dspring-boot.run.profiles=instance2
```

在 Eureka 中应看到同一 `employee-service` 有两个实例。

## 推荐启动顺序

Config → Eureka → 业务服务 → Gateway。

返回[示例总览](../../README.md)。
