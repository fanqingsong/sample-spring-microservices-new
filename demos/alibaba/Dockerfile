FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/maven:3.9.11-eclipse-temurin-21 AS maven-bin

FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/bellsoft/liberica-openjdk-debian:25 AS build
COPY --from=maven-bin /usr/share/maven /usr/share/maven
RUN ln -s /usr/share/maven/bin/mvn /usr/bin/mvn
WORKDIR /workspace
COPY docker/maven-settings.xml /root/.m2/settings.xml
COPY . .
RUN mvn -B -DskipTests package

FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/library/eclipse-temurin:25-jre AS runtime
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM runtime AS employee-service
COPY --from=build /workspace/employee-service/target/employee-service-*.jar /app/app.jar
EXPOSE 8080

FROM runtime AS department-service
COPY --from=build /workspace/department-service/target/department-service-*.jar /app/app.jar
EXPOSE 8080

FROM runtime AS organization-service
COPY --from=build /workspace/organization-service/target/organization-service-*.jar /app/app.jar
EXPOSE 8080

FROM runtime AS gateway-service
COPY --from=build /workspace/gateway-service/target/gateway-service-*.jar /app/app.jar
EXPOSE 8060
