#!/bin/bash
echo "🚀 Starting Notification Service with dependencies..."
docker-compose --profile notification up -d
echo "✅ Notification database started"
echo "🔄 Starting Notification Service..."
export $(cat .env | grep -v '^#' | xargs)
cd notification-service
mvn spring-boot:run