# ---- Build Stage ----
FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

RUN chmod +x gradlew

COPY src src

RUN SPRING_PROFILES_ACTIVE=test ./gradlew bootJar --no-daemon

# ---- Extract Stage (Layered JAR 추출 및 4개 레이어 분리) ----
FROM eclipse-temurin:25-jre AS extract

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

RUN java -Djarmode=layertools -jar app.jar extract

# ---- Run Stage (Docker Layer Cache 적용 및 최적화 실행) ----
FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=extract /app/dependencies/ ./
COPY --from=extract /app/spring-boot-loader/ ./
COPY --from=extract /app/snapshot-dependencies/ ./
COPY --from=extract /app/application/ ./

EXPOSE 8080

ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "org.springframework.boot.loader.launch.JarLauncher"]
