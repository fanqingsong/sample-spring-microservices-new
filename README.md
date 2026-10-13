# Spring Cloud Microservices Demos

This repository keeps every demo in a separate subdirectory so that all
examples can be browsed from the `master` branch without switching branches.
Each directory is a self-contained snapshot of the corresponding historical
branch.

## Available demos

- [`demos/master`](demos/master) — the current Spring Cloud microservices demo
- [`demos/springboot3`](demos/springboot3) — Spring Boot 3, Micrometer Tracing,
  and Springdoc
- [`demos/alibaba`](demos/alibaba) — Spring Cloud Alibaba Nacos, Zuul, and
  OpenFeign
- [`demos/graphql`](demos/graphql) — GraphQL, Apollo, and Eureka
- [`demos/openshift`](demos/openshift) — OpenShift Source-to-Image deployment
- [`demos/trampoline`](demos/trampoline) — local service management with
  Trampoline
- [`demos/kubernetes`](demos/kubernetes) — Kubernetes deployment example
- [`demos/rest-api-docs`](demos/rest-api-docs) — Spring REST Docs and
  Springfox Swagger comparison

The original documentation and usage instructions are retained inside each
demo directory.

## Running a demo

Change to the demo directory first, then follow its own README. For example,
the current demo can be started with:

```shell
cd demos/master
docker compose up --build
```

The demos are independent projects and are not intended to be built together
from the repository root.
