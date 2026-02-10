# 1. 빌드 스테이지
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew clean build -x test

# 2. 실행 스테이지 (경량화)
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# 빌드 스테이지에서 생성된 jar 파일 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 인프라 비용 최적화를 위해 컨테이너 내부 JVM 옵션 설정 (Xmx 조절)
ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]