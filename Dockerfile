# syntax=docker/dockerfile:1

# ---------- Сборка ----------
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Зависимости в отдельном слое: пока pom.xml не меняется, повторно не скачиваются.
# Кэш Maven между сборками хранится в BuildKit (--mount=type=cache).
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B dependency:go-offline

COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B clean package -DskipTests

# ---------- Запуск ----------
FROM eclipse-temurin:21-jre

WORKDIR /app

# Не запускаем приложение от root
RUN useradd --system --uid 1001 --no-create-home app

COPY --from=build /app/target/*.jar app.jar

USER app

EXPOSE 8888

# MaxRAMPercentage: JVM использует долю памяти контейнера, а не всей машины
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]