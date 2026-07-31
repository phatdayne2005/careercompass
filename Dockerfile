# ===== Stage 1: build =====
# Dùng JDK 21 đúng như <java.version> trong pom.xml.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy pom trước để Docker cache lớp dependency (chỉ tải lại khi pom đổi).
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Bỏ qua test ở đây: test đã chạy đầy đủ ở máy local (198/198 pass),
# và CareerCompassApplicationTests cần MySQL thật nên không chạy được trong build stage.
RUN mvn -B -q clean package -DskipTests

# ===== Stage 2: runtime =====
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Chạy bằng user thường thay vì root.
RUN addgroup -S app && adduser -S app -G app

COPY --from=build /build/target/*.jar app.jar

# Thư mục app ghi file lúc chạy (app.upload.dir = uploads/transcripts).
RUN mkdir -p /app/uploads/transcripts && chown -R app:app /app

USER app
EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
