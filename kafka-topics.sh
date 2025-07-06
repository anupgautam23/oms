#!/bin/bash
echo "📋 Managing Kafka Topics..."

# Create the order-events topic if it doesn't exist
echo "Creating order-events topic..."
docker exec kafka kafka-topics --bootstrap-server localhost:9092 --create --topic order-events --partitions 3 --replication-factor 1 --if-not-exists

echo "📋 Current topics:"
docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list

echo ""
echo "Topic details:"
docker exec kafka kafka-topics --bootstrap-server localhost:9092 --describe --topic order-events