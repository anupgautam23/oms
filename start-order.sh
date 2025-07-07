#!/bin/bash
echo "🚀 Starting Order Service with dependencies..."
docker-compose --profile order up -d
echo "✅ Order database started"
echo "🔄 Starting Order Service..."
export $(cat .env | grep -v '^#' | xargs)
cd order-service
mvn spring-boot:run