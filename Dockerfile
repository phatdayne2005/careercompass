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

# Gắn image vào repo: package sẽ hiện ở sidebar GitHub và thừa hưởng quyền của repo.
LABEL org.opencontainers.image.source=https://github.com/phatdayne2005/lap-trinh-java
LABEL org.opencontainers.image.description="CareerCompass - ứng dụng định hướng nghề nghiệp cho sinh viên IT"
LABEL org.opencontainers.image.licenses=Apache-2.0

# Chạy bằng user thường thay vì root.
RUN addgroup -S app && adduser -S app -G app

# Thư mục app ghi file lúc chạy (app.upload.dir = uploads/transcripts).
# Tạo TRƯỚC khi copy jar để chown chỉ đụng thư mục rỗng.
RUN mkdir -p /app/uploads/transcripts && chown -R app:app /app

# --chown ngay lúc copy. Nếu chown ở lệnh RUN sau, Docker phải chép lại
# toàn bộ jar sang layer mới -> image phình gấp đôi (~69MB thừa mỗi bản).
COPY --from=build --chown=app:app /build/target/*.jar app.jar

USER app
EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
