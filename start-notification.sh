#!/bin/bash
echo "🚀 Starting Notification Service with dependencies..."
docker-compose --profile notification up -d
echo "✅ Notification database started"
echo "🔄 Starting Notification Service..."
cd notification-service
./mvnw spring-boot:run