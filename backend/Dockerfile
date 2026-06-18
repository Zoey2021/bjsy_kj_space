# 多阶段构建：先用 Maven 编译，再运行 JAR
FROM maven:3.8-eclipse-temurin-8 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:8-jre
WORKDIR /app
COPY --from=build /app/target/learn-space-1.0.0.jar app.jar
# 【可修改】JVM 内存参数
ENV JAVA_OPTS="-Xms512m -Xmx1024m"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=docker"]
