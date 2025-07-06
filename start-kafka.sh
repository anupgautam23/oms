#!/bin/bash
echo "🚀 Starting Kafka and Zookeeper..."
docker-compose --profile kafka up -d

echo "⏳ Waiting for Kafka to be ready..."
sleep 15

echo "✅ Kafka is ready!"
echo ""
echo "Kafka is running on: localhost:9092"
echo "Zookeeper is running on: localhost:2181"
echo ""
echo "Test commands:"
echo "  • List topics: docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list"
echo "  • Create topic: docker exec kafka kafka-topics --bootstrap-server localhost:9092 --create --topic test-topic"