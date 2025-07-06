#!/bin/bash
echo "🚀 Starting Auth Service with dependencies..."
docker-compose --profile auth up -d
echo "✅ Auth database started"
echo "🔄 Starting Auth Service..."
cd auth-service
./mvnw spring-boot:run