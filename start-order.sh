#!/bin/bash
echo "🚀 Starting Order Service with dependencies..."
docker-compose --profile order up -d
echo "✅ Order database started"
echo "🔄 Starting Order Service..."
cd order-service
./mvnw spring-boot:run