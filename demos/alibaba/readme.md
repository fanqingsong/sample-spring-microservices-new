# Spring Cloud Alibaba：Nacos 微服务示例

## Demo 目标

本示例展示如何用 **Nacos** 作为注册中心，并与 **Spring Cloud Gateway**、**OpenFeign** 组成完整调用链：

- 业务服务与网关向 Nacos 注册；
- Gateway 通过 `lb://` 按服务名路由；
- Organization → Department → Employee 仍使用 OpenFeign 聚合。

代码已对齐 **Java 25、Spring Boot 4.1.1、Spring Cloud 2025.1.3、Spring Cloud Alibaba 2025.1.0.0**，不再使用 Zuul 或 `bootstrap.yml`。

## 快速开始（Docker Compose）

```shell
cd demos/alibaba
docker compose up --build
```

- Nacos 控制台：<http://localhost:8848/nacos>（默认无鉴权）
- Gateway：<http://localhost:8060>
- 聚合查询：`curl http://localhost:8060/organization/1/with-departments-and-employees`

## 与 master 的差异

| 项目 | alibaba | master |
| --- | --- | --- |
| 注册发现 | Nacos | Eureka |
| 配置 | 各服务 `application.yml` | Config Server |
| 网关 | Spring Cloud Gateway + Nacos | 同左 + Eureka |

## 模块

| 模块 | 作用 |
| --- | --- |
| `gateway-service` | 统一入口、OpenAPI 聚合 |
| `employee-service` | 员工服务 |
| `department-service` | 部门服务，Feign 调 employee |
| `organization-service` | 组织服务，Feign 调 department |

返回[示例总览](../../README.md)。
