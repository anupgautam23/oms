#!/bin/bash
echo "🚀 Starting All Services with dependencies..."
docker-compose --profile all up -d
echo "✅ All databases and Kafka started"
echo "🎉 All services ready!"
echo ""
echo "Now you can start individual services:"
echo "  • Auth Service: cd auth-service && ./mvnw spring-boot:run"
echo "  • Order Service: cd order-service && ./mvnw spring-boot:run"  
echo "  • Notification Service: cd notification-service && ./mvnw spring-boot:run"