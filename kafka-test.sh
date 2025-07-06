#!/bin/bash
echo "🧪 Testing Kafka Setup..."

# Check if Kafka is running
echo "1. Checking Kafka container..."
if docker ps | grep kafka > /dev/null; then
    echo "✅ Kafka container is running"
else
    echo "❌ Kafka container is not running"
    exit 1
fi

# List topics
echo "2. Listing topics..."
docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list

# Send test message
echo "3. Sending test message..."
echo "test-message-$(date)" | docker exec -i kafka kafka-console-producer --bootstrap-server localhost:9092 --topic order-events

echo "✅ Kafka test completed!"