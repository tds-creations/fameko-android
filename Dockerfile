# Stage 1: Build stage
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Copy the entire project context
# .dockerignore will handle excluding the large Android app modules
COPY . .

# Ensure gradlew is executable
RUN chmod +x gradlew

# Create dummy directories and files for ignored modules to satisfy Gradle
# This prevents Gradle from failing when it can't find the subprojects included in settings.gradle.kts
RUN mkdir -p app app-customer app-admin core && \
    touch app/build.gradle.kts app-customer/build.gradle.kts app-admin/build.gradle.kts core/build.gradle.kts

# Build the backend application
RUN ./gradlew :backend:installDist --no-daemon -Dorg.gradle.jvmargs="-Xmx384m"

# Stage 2: Runtime stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy built application from build stage
COPY --from=build /app/backend/build/install/backend /app/backend

# Use the PORT environment variable provided by Railway
ENV PORT=8080
# Set Java memory limits for the running app as well
ENV JAVA_OPTS="-Xmx256m"
EXPOSE $PORT

# Start the application
CMD ["sh", "-c", "/app/backend/bin/backend"]
