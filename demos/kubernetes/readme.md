# Kubernetes 微服务部署示例

## Demo 目标

本示例回答的是：**没有 Eureka / Config Server 时，如何用 Kubernetes Service DNS 把三个业务服务连起来，并分别演示两种常见的对外入口方式——Ingress 直连业务服务、Spring Cloud Gateway 聚合路由。** 数据在 MongoDB，不在内存仓库。

**重要说明：** Ingress 与 Gateway 在本清单里是**并列的两套演示**，**不是**必须配合使用。日常 curl 验证走 Ingress 路径即可；Gateway 用 port-forward 单独验证。两者不会在同一条请求链上串联。

要验证的几件事：

1. **东西向 — 平台 DNS 替代注册中心**：Pod 通过 Service 名 `employee`、`department`、`organization`、`mongodb` 互访（Feign / MongoDB；**不经过** Ingress 与 Gateway）。详见 [集群内东西向访问](#集群内东西向访问服务互访)。
2. **路径 A — Ingress**：外部前缀剥掉后直接打到业务 Service（当前清单**不经过 Gateway**）。
3. **路径 B — Gateway**：用固定 URI `http://employee:8080` 等转发，证明集群内服务名路由，而不是 `lb://` + Eureka。
4. **MongoDB 是空库**：必须先 POST 写入，再测查询；不能照搬 master 的 `organization/1/...`。

应用构建声明为 **Java 25、Spring Boot 4.1.1**；API 文档使用各服务自己的 Springdoc。清单里的镜像 tag 仍是历史值 `piomin/*:1.0`，部署前需自行构建并改名。

## 架构

```mermaid
flowchart TB
    subgraph pathA["路径 A · Ingress 直连（默认 curl，见验证 §2–§3）"]
        BrowserA[客户端] -->|Host: microservices.info| Ingress[Ingress nginx]
        Ingress -->|/employee| EmpSvc
        Ingress -->|/department| DeptSvc
        Ingress -->|/organization| OrgSvc
    end

    subgraph pathB["路径 B · Gateway 聚合（port-forward，见验证 §4）"]
        BrowserB[客户端] --> GW[Service gateway]
        GW --> Gateway[gateway Pod]
        Gateway -->|http://employee:8080| EmpSvc
        Gateway -->|http://department:8080| DeptSvc
        Gateway -->|http://organization:8080| OrgSvc
    end

    subgraph backends["三个业务 Service → Pod（路径 A / B 共用后端）"]
        EmpSvc[Service employee] --> Emp[employee Pod]
        DeptSvc[Service department] --> Dept[department Pod]
        OrgSvc[Service organization] --> Org[organization Pod]
    end

    Dept -->|Feign name=employee| EmpSvc
    Org -->|Feign name=department| DeptSvc
    Emp --> Mongo[(Service mongodb)]
    Dept --> Mongo
    Org --> Mongo
```

**图例（路径 A / B 互不串联）：**

| 标记 | 入口 | 本清单中的访问方式 | 说明 |
| --- | --- | --- | --- |
| **路径 A** | Ingress-NGINX | `http://<INGRESS_IP>/employee/` + `Host: microservices.info` | NGINX 按前缀选 Service 并 `rewrite-target`；**不经过** Gateway Pod |
| **路径 B** | Spring Cloud Gateway | `kubectl port-forward svc/gateway 8080:8080` 后访问 `http://127.0.0.1:8080/employee/` | 路由在 `application.yml`，目标 URI 为 K8s 服务名；**不经过** Ingress |
| **东西向** | Pod ↔ Pod（Feign / MongoDB） | 不经过 Ingress 与 Gateway | 见下文 [集群内东西向访问](#集群内东西向访问服务互访) |
| **共用** | 业务 Service / Pod | — | 南北向（A/B）最终都打到同一组 Service；**集群内互访**走另一条路径，直接 `Service:8080` |

图中 **路径 A 与路径 B 之间没有箭头**：一次请求只走其中一条。第三种组合「Ingress → Gateway → 微服务」见下文「可选：Ingress 指到 Gateway」。**服务之间的调用**在架构图里用 Feign / Mongo 箭头表示，与 A、B 无关。

## 集群内东西向访问（服务互访）

上文路径 A、B 讲的是 **南北向**：集群**外**的客户端如何进来。微服务在集群**内部**互相调用时，通常 **既不经过 Ingress，也不经过 Gateway**，而是 Pod（或同命名空间内的进程）通过 **Kubernetes Service + 集群 DNS（CoreDNS）** 直接访问对方。

### 和南北向的区别

| 维度 | 南北向（路径 A / B） | 东西向（本节目） |
| --- | --- | --- |
| **谁发起** | 集群外的浏览器、curl、公网 LB 等 | 另一个 Pod 里的应用（如 department 调 employee） |
| **经过谁** | Ingress 和/或 Gateway | 仅 **ClusterIP Service → 目标 Pod** |
| **URL 形态** | 对外常带 `/employee`、`/department` 等前缀，入口层负责剥前缀 | 业务 API 在**根路径**（如 `GET /department/{id}`），**没有** `/employee/...` 这类对外前缀 |
| **“发现”方式** | Ingress 规则或 Gateway 路由表 | **Service 名即主机名**（同 namespace 下短名 `employee` 即可） |

### DNS 与 Service 名

清单里每个 workload 都有同名 Service（见 `kubernetes/*-deployment.yaml` 中 `---` 后的 `kind: Service`）：

| Service 名 | 用途 |
| --- | --- |
| `employee` | 员工 API，端口 8080 |
| `department` | 部门 API，端口 8080 |
| `organization` | 组织 API，端口 8080 |
| `mongodb` | MongoDB，连接串里作主机名（见各服务 `application.yml` 的 `spring.data.mongodb.uri`） |

Pod 内解析 `employee` 会得到 ClusterIP，流量由 kube-proxy / CNI 转到后端 Pod。完整 DNS 一般为 `employee.<namespace>.svc.cluster.local`；本 Demo 默认同一 namespace，代码和配置里都用**短名**。

### Feign 调用链（替代 Eureka）

`department-service`、`organization-service` 启用 OpenFeign。`@FeignClient` 的 **name 与 K8s Service 名一致**（不是 master 里的 `employee-service` 注册名），从而在集群内把「逻辑服务 id」映射到 **DNS 可解析的 Service**：

| 调用方 | Feign 接口 | `name` | 典型被调 API（employee 等服务的根路径） |
| --- | --- | --- | --- |
| department | `EmployeeClient` | `employee` | `GET /department/{departmentId}` |
| organization | `DepartmentClient` | `department` | `GET /organization/{organizationId}/with-employees` 等 |
| organization | `EmployeeClient` | `employee` | `GET /organization/{organizationId}` |

聚合示例：外部 `GET .../department/organization/1/with-employees`（经 Ingress 路径 A 进 department）在 department Pod 内会再 **Feign 调 employee**，形成 **Ingress → department Service → department Pod → employee Service → employee Pod**；中间两段 **department → employee** 就是东西向，与 Gateway 无关。

MongoDB 访问同理：`mongodb://${MONGO_USERNAME}:...@mongodb/${MONGO_DATABASE}`，属于 **Pod → mongodb Service** 的东西向，不走 Feign。

```mermaid
flowchart LR
    subgraph eastWest["东西向（集群内，无 Ingress / Gateway）"]
        DeptPod[department Pod]
        OrgPod[organization Pod]
        EmpPod[employee Pod]
        DeptPod -->|Feign name=employee| EmpSvc[Service employee]
        OrgPod -->|Feign name=department| DeptSvc[Service department]
        OrgPod -->|Feign name=employee| EmpSvc
        EmpSvc --> EmpPod
        DeptSvc --> DeptPod
        DeptPod --> MongoSvc[Service mongodb]
        EmpPod --> MongoSvc
        OrgPod --> MongoSvc
    end
```

### 和 master 的对应关系

| master | kubernetes 东西向 |
| --- | --- |
| Eureka 注册名 `employee-service` 等 | Service 名 `employee` 等 |
| Feign + `lb://` | Feign `name` = Service 名 + 集群 DNS |
| 服务间也走根路径 API | 相同；仅对外入口才加 `/employee` 前缀 |

**验证东西向：** 在路径 A 写入数据后，请求会触发 Feign 的接口即可（见「验证」§3 中 `GET .../department/organization/1/with-employees`）。若返回里带 `employees`，说明 department Pod 已成功通过 Service `employee` 访问 employee Pod。

## Ingress 与 Gateway 的分工

### 两种并列演示，并非必须配合

本 Demo **同时部署** Ingress 规则和 Gateway Pod，但**默认不把二者接在同一条流量链上**。目的是让你分别验证两种「从集群外访问三个微服务」的做法，而不是教你在同一份 YAML 里必须叠两层网关。

| 路径 | 请求链 | 在本 README 里怎么测 |
| --- | --- | --- |
| **路径 A（默认）** | 客户端 → **Ingress** → employee / department / organization Service | 带 `Host: microservices.info` 访问 `<INGRESS_IP>/employee/` 等（见下文「验证」§2–§3） |
| **路径 B（对照）** | 客户端 → **Gateway** → 三个业务 Service | `kubectl port-forward svc/gateway 8080:8080` 后访问 `http://127.0.0.1:8080/employee/` 等（见下文「验证」§4） |

两条路径在**效果上类似**（都是 `/employee` 等前缀 + 剥前缀后打到各服务根路径 API），但**清单里故意断开**：路径 A 不经过 Gateway Pod；路径 B 不经过 Ingress。因此：

- **不是说**「Ingress 和 Gateway 要一起用才能完成路由」；
- **而是说**「同一种 K8s DNS 发现能力，可以用平台 Ingress 实现，也可以用应用网关实现，本仓库两种都留了代码和清单供你对照」。

**生产里常见的第三种组合**（本 Demo **未**采用为默认）是 **Ingress → Gateway → 微服务**：只保留一个对外域名/入口，由 Ingress 把流量交给 Gateway，Gateway 再做鉴权、限流、统一路由等。若要与 [master](../master/readme.md) 一样「只从一个网关端口进」，属于**可选改造**（见下文「可选：Ingress 指到 Gateway」），不是当前 `ingress.yaml` 的行为。

### 职责对比

在本 Demo 里，Ingress 与 Gateway 都能把带前缀的 URL（如 `/employee/...`）转成业务服务根路径上的 API，但所在层级、职责和默认流量路径不同。

| 维度 | Ingress（集群入口） | Gateway（Spring Cloud Gateway） |
| --- | --- | --- |
| **层级** | Kubernetes / Ingress-NGINX，集群边界 | 应用 Pod，与 employee 等同级微服务 |
| **主要职责** | 把集群外 HTTP 流量接进来：按 **Host**（`microservices.info`）和 **路径前缀** 选后端 Service，并在 NGINX 侧做前缀剥离 | 在应用层做**统一路由**：按路径转发到 `http://employee:8080` 等固定 URI，并用 `RewritePath` 剥前缀 |
| **服务发现** | 不解析 Eureka；后端写死为 K8s **Service 名 + 端口**（`employee:8080` 等） | 同样用 K8s DNS 服务名（见 `gateway-service/.../application.yml`），**不是** `lb://` + 注册中心 |
| **本清单中的流量** | **默认入口**：`ingress.yaml` 直连三个业务 Service，**不经过** Gateway Pod | **未挂到 Ingress**：通过 Service 的 NodePort 或 `kubectl port-forward svc/gateway` 访问 |
| **典型用途** | 生产里常见的「一个域名 + 多路径 → 多 Service」 | 与 [master](../master/readme.md) 对齐：单一网关端口聚合 API；此处仅演示「网关 + K8s DNS」可替代「网关 + Eureka」 |

**Ingress 负责什么：** 平台级南北向流量。客户端只需知道 Ingress 地址和 Host，例如 `GET http://<INGRESS_IP>/employee/`；Controller 匹配 `/employee(/|$)(.*)`，按注解 `rewrite-target: /$2` 把请求改成后端的 `GET /`，再转发到 `Service employee`。

**Gateway 负责什么：** 业务侧网关能力（路由表、后续可扩展鉴权、限流、链路头等）。对外仍暴露 `/employee/**` 等路径，但转发与改写由 Spring Cloud Gateway 完成（配置见 `gateway-service/src/main/resources/application.yml`）。路径 B 单独访问 Gateway，是为了对照 master 的「统一网关」模型，并确认路由目标写的是 K8s 服务名而非 Eureka。

### 可选：Ingress 指到 Gateway（第三种组合）

若希望与 master 一样「只从一个网关进」，可把 `kubernetes/ingress.yaml` 的后端改为 `gateway:8080`（单条或少量规则），并保留 Gateway 里现有的三条路由。此时 Ingress 通常只做 Host/TLS/路径粗分，**避免**与 Gateway 的 `RewritePath` **重复剥同一层前缀**（否则容易 404）。这是生产常见拓扑的说明，**不是**本示例默认要完成的验证项。

## 与 master 的差异

| 项目 | kubernetes | master |
| --- | --- | --- |
| 发现（东西向） | K8s Service DNS + Feign `name` 对齐 Service | Eureka |
| 配置 | 各服务 `application.yml` + ConfigMap/Secret | Config Server |
| 网关路由 | `http://employee:8080` | `lb://employee-service` |
| 入口 | Ingress 直连三业务服务；Gateway 需 NodePort / port-forward | 统一 8060 |
| 数据 | MongoDB，启动为空 | 内存 + 种子数据 |

## 模块与清单

| 清单 | 作用 |
| --- | --- |
| `kubernetes/mongodb-configmap.yaml`、`mongodb-secret.yaml` | 库名 `admin`，用户/密码 |
| `kubernetes/mongodb-deployment.yaml` | MongoDB Deployment + Service |
| `kubernetes/employee-deployment.yaml` 等 | 三业务 + gateway 的 Deployment/Service |
| `kubernetes/ingress.yaml` | Host `microservices.info`，class `nginx` |

业务 API 在**根路径**（无 `/employee` 类 `@RequestMapping`）。Ingress 用 `rewrite-target: /$2` 去掉前缀；Gateway 同样 RewritePath。

## 运行方式

### 前置条件

- 可用的 Kubernetes 集群与 `kubectl`；
- 集群已安装 Ingress-NGINX（`ingressClassName: nginx`）；
- 能构建并推送（或 `kind load` / `minikube image load`）应用镜像。

工作目录：`demos/kubernetes`。

### 1. 构建并对齐镜像名

清单期望 `piomin/employee:1.0`、`piomin/department:1.0`、`piomin/organization:1.0`、`piomin/gateway:1.0`。请按当前 POM 打出 JAR/镜像后 **tag 成上述名字**，或改 YAML 中的 `image:`。各业务模块下的 Dockerfile 仍是历史 `openjdk:8` + `1.0-SNAPSHOT.jar`，与 Java 25 / `1.2-SNAPSHOT` 不一致时，优先用 Spring Boot 构建镜像并改 YAML。

### 2. 按依赖 apply

```shell
kubectl apply -f kubernetes/mongodb-configmap.yaml
kubectl apply -f kubernetes/mongodb-secret.yaml
kubectl apply -f kubernetes/mongodb-deployment.yaml
kubectl apply -f kubernetes/employee-deployment.yaml
kubectl apply -f kubernetes/department-deployment.yaml
kubectl apply -f kubernetes/organization-deployment.yaml
kubectl apply -f kubernetes/gateway-deployment.yaml
kubectl apply -f kubernetes/ingress.yaml
```

```shell
kubectl get pods
kubectl get svc
kubectl get ingress
```

四个应用 Pod 与 `mongodb` 应为 Running；业务 Pod 依赖 Secret/ConfigMap，漏 apply 会起不来。

## 验证

以下 **§2–§3 测路径 A（Ingress）**，**§4 测路径 B（Gateway）**；二者独立，任选或都做均可。

把 `<INGRESS_IP>` 换成 Ingress 地址。若本机无法解析 `microservices.info`，所有 curl 都加 `-H "Host: microservices.info"`。

### 1. 工作负载就绪

```shell
kubectl get pods
kubectl get svc employee department organization gateway mongodb
```

**通过标准：** 上述 Service 存在；Mongo 与三个业务 Pod `Ready`。可进业务 Pod 确认能解析 `mongodb`：

```shell
kubectl exec -it deploy/employee -- sh -c "getent hosts mongodb || nslookup mongodb"
```

### 2. 空库 + Ingress 前缀剥离

```shell
curl -s -H "Host: microservices.info" http://<INGRESS_IP>/employee/
```

应返回 `[]`（空数组），**不是** 404。这说明 Ingress 已把 `/employee/` 改写成后端的 `GET /`。部门、组织同理：

```shell
curl -s -H "Host: microservices.info" http://<INGRESS_IP>/department/
curl -s -H "Host: microservices.info" http://<INGRESS_IP>/organization/
```

### 3. 写入后再查询（Mongo 无种子数据）

```shell
curl -s -H "Host: microservices.info" -H "Content-Type: application/json" \
  -d '{"name":"Microsoft","address":"Redmond"}' \
  http://<INGRESS_IP>/organization/

curl -s -H "Host: microservices.info" -H "Content-Type: application/json" \
  -d '{"organizationId":1,"name":"Development"}' \
  http://<INGRESS_IP>/department/

curl -s -H "Host: microservices.info" -H "Content-Type: application/json" \
  -d '{"organizationId":1,"departmentId":1,"name":"John Smith","age":34,"position":"Analyst"}' \
  http://<INGRESS_IP>/employee/
```

再 `GET /employee/`、`GET /department/`，应能看到刚写入的文档。Organization 的 Mongo `id` 是字符串；Department / Employee 的 `organizationId` 在本示例中是 **Long**，跨服务按组织聚合时不要假设能用 Organization 的 ObjectId 去对上。可验证部门侧：

```shell
curl -s -H "Host: microservices.info" \
  http://<INGRESS_IP>/department/organization/1/with-employees
```

若 Feign 与 DNS 正常，Development 的 `employees` 中应出现 `John Smith`。（该请求在 Ingress 进入 department 之后，**集群内**还会东西向调用 employee，见 [集群内东西向访问](#集群内东西向访问服务互访)。）

### 4. Gateway 走集群内服务名（路径 B，与 Ingress 无关）

本节验证的是**并列的另一条入口**，不要求先走 Ingress。Ingress **没有**把流量交给 Gateway。要验证 Gateway 配置里的 `http://employee:8080`：

```shell
kubectl port-forward svc/gateway 8080:8080
curl -s http://127.0.0.1:8080/employee/
curl -s http://127.0.0.1:8080/department/
```

应与 Ingress 直连结果同类（列表或刚写入的数据）。这能说明 Gateway 用的是 K8s DNS，而不是 Eureka。

## 已知限制

- README 旧部署命令漏了 ConfigMap/Secret，上面的顺序已补上。
- **Ingress 与 Gateway 是两种并列演示**，默认清单下 Ingress 直连业务服务；Gateway 仅通过 NodePort / port-forward 参与路径 B，二者不在同一条请求链上串联。
- 镜像名、Dockerfile JDK、JAR 版本与当前 POM 不一致，必须先对齐再部署。
- Organization 的 String Mongo id 与 Department/Employee 的 Long `organizationId` 类型不同，完整的 `/{orgId}/with-departments-and-employees` 不能按 master 的数字 `1` 照搬。
- Ingress 注解与 `ingressClassName` 需与集群 Controller 匹配。

## 延伸阅读

- 对照平台内构建：[openshift](../openshift/readme.md)
- 对照注册中心方案：[master](../master/readme.md)
- 返回[示例总览](../../README.md)
