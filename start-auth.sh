#!/bin/bash
echo "🚀 Starting Auth Service with dependencies..."
docker-compose --profile auth up -d
echo "✅ Auth database started"
echo "🔄 Starting Auth Service..."
export $(cat .env | grep -v '^#' | xargs)
cd auth-service
mvn spring-boot:run