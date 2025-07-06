#!/bin/bash
echo "👁️ Monitoring Kafka Messages..."
echo "Listening to order-events topic..."
echo "Press Ctrl+C to stop"
echo ""

docker exec -it kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic order-events --from-beginning