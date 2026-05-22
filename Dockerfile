# syntax=docker/dockerfile:1
# ============================================================
# Empacotamento do Last Mile — build multi-stage.
# Resultado: uma imagem única que serve a API + o frontend React.
# ============================================================

# ------------------------------------------------------------
# Stage 1 — build do frontend React (Vite)
# ------------------------------------------------------------
FROM node:20-alpine AS frontend
WORKDIR /app/frontend
# instala as dependências primeiro (camada cacheável)
COPY frontend/package.json frontend/package-lock.json* ./
# --legacy-peer-deps: necessário pelo conflito react-leaflet x React 19
RUN npm install --legacy-peer-deps
COPY frontend/ ./
RUN npm run build

# ------------------------------------------------------------
# Stage 2 — build do JAR (Maven + Java 21), com o frontend embutido
# ------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /app
COPY pom.xml ./
COPY src ./src
# coloca o build do React dentro dos recursos estáticos do Spring Boot
COPY --from=frontend /app/frontend/dist/ ./src/main/resources/static/
# os 22 testes precisam de Postgres/Redis; cobertura mantida via 'mvn test' local
RUN mvn -q clean package -DskipTests

# ------------------------------------------------------------
# Stage 3 — imagem final enxuta (apenas o runtime Java)
# ------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
# roda como usuário sem privilégios (boa prática de segurança)
RUN addgroup -S app && adduser -S app -G app
COPY --from=backend /app/target/optiroute-0.0.1-SNAPSHOT.jar app.jar
RUN chown app:app app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
