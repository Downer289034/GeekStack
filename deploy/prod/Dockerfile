# 后端运行镜像：仅携带 jar + JRE 17（构建在 CI/本机完成后复制产物）
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY community-backend-*.jar app.jar
# 日志落盘目录
RUN mkdir -p /app/logs
ENV TZ=Asia/Shanghai
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
