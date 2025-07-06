# Order Management System (OMS)

A comprehensive microservices-based Order Management System built with Spring Boot, PostgreSQL, and Apache Kafka.

## 📋 Table of Contents

- [Architecture Overview](#architecture-overview)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Environment Setup](#environment-setup)
- [Services](#services)
- [Database Configuration](#database-configuration)
- [Kafka Configuration](#kafka-configuration)
- [API Documentation](#api-documentation)
- [Development](#development)
- [Testing](#testing)
- [Security](#security)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)

## 🏗️ Architecture Overview

The OMS consists of three microservices that communicate via REST APIs and Kafka events:

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Auth Service  │    │  Order Service  │    │Notification Svc │
│   (Port 8081)   │    │   (Port 8082)   │    │   (Port 8083)   │
│                 │    │                 │    │                 │
│ • User Auth     │    │ • Order CRUD    │    │ • Email Alerts  │
│ • JWT Tokens    │    │ • Order Events  │    │ • Event Processing│
│ • User Management│   │ • Inventory     │    │ • Notifications │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Auth DB       │    │   Order DB      │    │Notification DB  │
│  (Port 5433)    │    │  (Port 5434)    │    │  (Port 5435)    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                                ▼
                    ┌─────────────────────────┐
                    │    Apache Kafka         │
                    │  (Port 9092)            │
                    │                         │
                    │ • order-events topic    │
                    │ • Event-driven comms    │
                    └─────────────────────────┘
                                │
                                ▼
                    ┌─────────────────────────┐
                    │      Zookeeper          │
                    │     (Port 2181)         │
                    └─────────────────────────┘
```

## 🔧 Prerequisites

- **Java 17** or higher
- **Maven 3.8+**
- **Docker** and **Docker Compose**
- **Git**
- **TablePlus** or similar database client (optional)

## 🚀 Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/anupgautam23/oms.git
cd oms
```

### 2. Set Up Environment Variables

```bash
# Copy the environment template
cp .env.example .env

# Edit .env with your preferred settings (optional for local development)
# The default values work fine for local development
```

### 3. Make Scripts Executable

```bash
chmod +x *.sh
```

### 4. Start All Infrastructure

```bash
# Start all databases and Kafka
./start-all.sh
```

### 5. Start Individual Services

**Terminal 1 - Auth Service:**
```bash
cd auth-service
./mvnw spring-boot:run
```

**Terminal 2 - Order Service:**
```bash
cd order-service
./mvnw spring-boot:run
```

**Terminal 3 - Notification Service:**
```bash
cd notification-service
./mvnw spring-boot:run
```

### 6. Verify Everything is Running

```bash
# Check all containers
docker ps

# Test services
curl http://localhost:8081/api/auth/health
curl http://localhost:8082/api/orders/health
curl http://localhost:8083/api/notifications/health
```

## ⚙️ Environment Setup

### Environment Variables

The project uses environment variables for configuration. Copy the example file and customize as needed:

```bash
cp .env.example .env
```

### Available Environment Variables

| Variable | Description | Default Value |
|----------|-------------|---------------|
| `AUTH_DB_PASSWORD` | Auth database password | `*****` |
| `ORDER_DB_PASSWORD` | Order database password | `****` |
| `NOTIFICATION_DB_PASSWORD` | Notification database password | `****` |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap servers | `localhost:9092` |
| `JWT_SECRET` | JWT signing secret | (set in production) |

### 📁 Project Structure

```
oms/
├── auth-service/           # Authentication microservice
├── order-service/          # Order management microservice
├── notification-service/   # Notification microservice
├── docker-compose.yml      # Infrastructure setup
├── .env.example           # Environment template
├── .env                   # Your environment variables (gitignored)
├── start-all.sh           # Start all containers
├── start-kafka.sh         # Start Kafka only
├── kafka-*.sh             # Kafka management scripts
└── README.md
```

## 🏢 Services

### Auth Service (Port 8081)
- **Database:** PostgreSQL (Port 5433)
- **Features:** User registration, authentication, JWT tokens
- **Auto-starts:** `auth-db` container

### Order Service (Port 8082)
- **Database:** PostgreSQL (Port 5434)
- **Features:** Order management, inventory tracking, event publishing
- **Auto-starts:** `order-db` + `kafka` + `zookeeper` containers

### Notification Service (Port 8083)
- **Database:** PostgreSQL (Port 5435)
- **Features:** Event processing, email notifications, notification history
- **Auto-starts:** `notification-db` + `kafka` + `zookeeper` containers

## 🗄️ Database Configuration

### Connection Details

| Service | Database | Host | Port | User | Password |
|---------|----------|------|------|------|----------|
| Auth | authdb | localhost | 5433 | postgres | `${AUTH_DB_PASSWORD}` |
| Order | orderdb | localhost | 5434 | postgres | `${ORDER_DB_PASSWORD}` |
| Notification | notificationdb | localhost | 5435 | postgres | `${NOTIFICATION_DB_PASSWORD}` |

**Note:** Passwords are configured via environment variables in `.env` file.

### Connect with Database Client (TablePlus)

1. **Auth Database:**
   ```bash
   Host: localhost
   Port: 5433
   Database: authdb
   User: postgres
   Password: (check your .env file)
   ```

2. **Order Database:**
   ```bash
   Host: localhost
   Port: 5434
   Database: orderdb
   User: postgres
   Password: (check your .env file)
   ```

3. **Notification Database:**
   ```bash
   Host: localhost
   Port: 5435
   Database: notificationdb
   User: postgres
   Password: (check your .env file)
   ```

### Database Quick Connect

```bash
# Connect to Auth DB
docker exec -it auth-db psql -U postgres -d authdb

# Connect to Order DB
docker exec -it order-db psql -U postgres -d orderdb

# Connect to Notification DB
docker exec -it notification-db psql -U postgres -d notificationdb
```

## 📨 Kafka Configuration

### Kafka Details
- **Broker:** `localhost:9092`
- **Zookeeper:** `localhost:2181`
- **Topic:** `order-events`

### Kafka Management Scripts

```bash
# Start Kafka only
./start-kafka.sh

# Create and manage topics
./kafka-topics.sh

# Monitor messages in real-time
./kafka-monitor.sh

# Test Kafka setup
./kafka-test.sh
```

### Kafka Commands Reference

```bash
# List topics
docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list

# Create topic
docker exec kafka kafka-topics --bootstrap-server localhost:9092 --create --topic order-events --partitions 3 --replication-factor 1

# Send test message
echo "test message" | docker exec -i kafka kafka-console-producer --bootstrap-server localhost:9092 --topic order-events

# Consume messages
docker exec kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic order-events --from-beginning
```

## 📚 API Documentation

### Auth Service APIs

```bash
# Health check
GET http://localhost:8081/api/auth/health

# Register user
POST http://localhost:8081/api/auth/register
Content-Type: application/json

{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "password123"
}

# Login
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
  "username": "john_doe",
  "password": "password123"
}
```

### Order Service APIs

```bash
# Health check
GET http://localhost:8082/api/orders/health

# Get all orders
GET http://localhost:8082/api/orders

# Create order
POST http://localhost:8082/api/orders
Content-Type: application/json

{
  "productName": "Laptop",
  "quantity": 1,
  "price": 999.99,
  "customerEmail": "customer@example.com"
}

# Get order by ID
GET http://localhost:8082/api/orders/{id}

# Update order
PUT http://localhost:8082/api/orders/{id}
Content-Type: application/json

{
  "status": "SHIPPED"
}
```

### Notification Service APIs

```bash
# Health check
GET http://localhost:8083/api/notifications/health

# Get all notifications
GET http://localhost:8083/api/notifications

# Get notifications by email
GET http://localhost:8083/api/notifications/email/{email}
```

## 🛠️ Development

### Available Scripts

| Script | Description |
|--------|-------------|
| `./start-all.sh` | Start all databases and Kafka |
| `./start-kafka.sh` | Start only Kafka and Zookeeper |
| `./kafka-topics.sh` | Create and manage Kafka topics |
| `./kafka-monitor.sh` | Monitor Kafka messages in real-time |
| `./kafka-test.sh` | Test Kafka setup |

### Development Workflow

1. **Start Infrastructure:**
   ```bash
   ./start-all.sh
   ```

2. **Run Service in Development Mode:**
   ```bash
   cd {service-name}
   ./mvnw spring-boot:run
   ```

3. **Auto-restart on Changes:**
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=dev"
   ```

### Docker Compose Profiles

The system uses Docker Compose profiles to manage service dependencies:

```bash
# Start only auth service dependencies
docker-compose --profile auth up -d

# Start only order service dependencies
docker-compose --profile order up -d

# Start only notification service dependencies
docker-compose --profile notification up -d

# Start Kafka and Zookeeper
docker-compose --profile kafka up -d

# Start everything
docker-compose --profile all up -d
```

## 🧪 Testing

### Unit Tests

```bash
# Run tests for a specific service
cd auth-service
./mvnw test

# Run tests with coverage
./mvnw test jacoco:report
```

### Integration Tests

```bash
# Start infrastructure first
./start-all.sh

# Run integration tests
./mvnw test -P integration-tests
```

### End-to-End Testing

```bash
# 1. Start all services
./start-all.sh

# 2. Register a user
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"password123"}'

# 3. Create an order
curl -X POST http://localhost:8082/api/orders \
  -H "Content-Type: application/json" \
  -d '{"productName":"Test Product","quantity":1,"price":99.99,"customerEmail":"test@example.com"}'

# 4. Check notifications
curl http://localhost:8083/api/notifications/email/test@example.com
```

## 🔐 Security

### Security Features
- **JWT Authentication:** All API endpoints (except health checks) require valid JWT tokens
- **Password Encryption:** User passwords are encrypted using BCrypt
- **Database Security:** Each service has its own database with unique credentials
- **Network Isolation:** Services communicate through a custom Docker network
- **Environment Variables:** Sensitive data is stored in environment variables

### Security Configuration

#### Local Development
1. **Environment Setup:**
   ```bash
   cp .env.example .env
   # Edit .env with your local settings
   ```

2. **Default Settings:**
   - Simple passwords for development convenience
   - Services only accessible on localhost
   - Not suitable for production use

#### Production Deployment
**⚠️ Important Security Guidelines:**
- **Never** commit real passwords to version control
- Use strong, unique passwords for each service
- Enable database SSL/TLS connections
- Use secrets management systems (AWS Secrets Manager, HashiCorp Vault, etc.)
- Regularly rotate credentials
- Set up proper firewall rules
- Use HTTPS for all API endpoints

#### Environment Variables for Production
```bash
# Production .env example
AUTH_DB_PASSWORD=very_secure_random_password_123!
ORDER_DB_PASSWORD=another_secure_password_456!
NOTIFICATION_DB_PASSWORD=yet_another_secure_password_789!
JWT_SECRET=very_long_and_random_jwt_secret_key_for_production
KAFKA_BOOTSTRAP_SERVERS=your-kafka-cluster:9092
```

## 🔍 Troubleshooting

### Common Issues

#### 1. Port Already in Use
```bash
# Check what's using the port
lsof -i :8081
lsof -i :9092

# Kill the process
sudo lsof -ti:8081 | xargs kill -9
```

#### 2. Database Connection Failed
```bash
# Check if database containers are running
docker ps | grep postgres

# Check database logs
docker logs auth-db
docker logs order-db
docker logs notification-db

# Verify environment variables
cat .env
```

#### 3. Kafka Not Starting
```bash
# Check Kafka logs
docker logs kafka
docker logs zookeeper

# Restart Kafka
docker-compose down kafka zookeeper
./start-kafka.sh
```

#### 4. Service Won't Start
```bash
# Check if all dependencies are running
docker ps

# Check for Maven wrapper permissions
chmod +x mvnw

# Check service logs
cd {service-name}
./mvnw spring-boot:run --debug
```

#### 5. Permission Denied on Scripts
```bash
# Make all scripts executable
chmod +x *.sh

# Or run with bash
bash start-kafka.sh
```

#### 6. Environment Variables Not Loading
```bash
# Check if .env file exists
ls -la .env

# Verify environment variables
docker-compose config

# Restart containers after .env changes
docker-compose down
./start-all.sh
```

### Useful Commands

```bash
# View all containers
docker ps -a

# View container logs
docker logs {container-name}

# Follow logs in real-time
docker logs -f {container-name}

# Stop all containers
docker-compose down

# Clean up everything
docker-compose down -v
docker system prune -a

# Check Docker Compose configuration
docker-compose config
```

## 📊 Monitoring

### Health Checks

```bash
# All services health
curl http://localhost:8081/api/auth/health
curl http://localhost:8082/api/orders/health
curl http://localhost:8083/api/notifications/health

# Database connections
docker exec auth-db pg_isready
docker exec order-db pg_isready
docker exec notification-db pg_isready

# Kafka status
./kafka-test.sh
```

### Container Status

```bash
# Check all containers
docker ps

# Expected containers when everything is running:
# - auth-db (postgres:15)
# - order-db (postgres:15)
# - notification-db (postgres:15)
# - zookeeper (confluentinc/cp-zookeeper:latest)
# - kafka (confluentinc/cp-kafka:7.4.0)
```

### System Health Check Script

```bash
#!/bin/bash
echo "🔍 OMS System Health Check"
echo "=========================="

# Check containers
echo "📦 Container Status:"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo -e "\n🌐 Service Health:"
# Check services
curl -s http://localhost:8081/api/auth/health && echo "✅ Auth Service: OK" || echo "❌ Auth Service: FAILED"
curl -s http://localhost:8082/api/orders/health && echo "✅ Order Service: OK" || echo "❌ Order Service: FAILED"
curl -s http://localhost:8083/api/notifications/health && echo "✅ Notification Service: OK" || echo "❌ Notification Service: FAILED"

echo -e "\n📊 Database Status:"
docker exec auth-db pg_isready && echo "✅ Auth DB: OK" || echo "❌ Auth DB: FAILED"
docker exec order-db pg_isready && echo "✅ Order DB: OK" || echo "❌ Order DB: FAILED"
docker exec notification-db pg_isready && echo "✅ Notification DB: OK" || echo "❌ Notification DB: FAILED"

echo -e "\n📨 Kafka Status:"
./kafka-test.sh
```

## 🤝 Contributing

1. **Fork the repository**
2. **Create a feature branch:** `git checkout -b feature/new-feature`
3. **Make your changes and commit:** `git commit -m "Add new feature"`
4. **Push to the branch:** `git push origin feature/new-feature`
5. **Create a Pull Request**

### Code Style

- Follow **Java 17** best practices
- Use **Spring Boot** conventions
- Write **unit tests** for new features
- Update **documentation** for API changes
- Follow **RESTful API** design principles

### Development Guidelines

- Each service should be independent
- Use events for inter-service communication
- Implement proper error handling
- Add logging for debugging
- Write comprehensive tests
- Never commit sensitive data (passwords, secrets)

---

## 📞 Support

If you encounter any issues:

1. Check the [Troubleshooting](#troubleshooting) section
2. Review application logs
3. Verify all containers are running with `docker ps`
4. Test individual components using the provided scripts
5. Check your `.env` file configuration
6. Create an issue in the repository

### Quick Debug Commands

```bash
# Full system check
docker ps
./kafka-test.sh
curl http://localhost:8081/api/auth/health
curl http://localhost:8082/api/orders/health
curl http://localhost:8083/api/notifications/health

# Check environment variables
cat .env
docker-compose config

# Reset everything
docker-compose down -v
./start-all.sh
```

---

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

---

**Happy Coding! 🚀**

*Built with ❤️ using Spring Boot, PostgreSQL, and Apache Kafka*

---

## 🔖 Quick Reference

### Ports
- **Auth Service:** 8081
- **Order Service:** 8082
- **Notification Service:** 8083
- **Auth DB:** 5433
- **Order DB:** 5434
- **Notification DB:** 5435
- **Kafka:** 9092
- **Zookeeper:** 2181

### Key Files
- `docker-compose.yml` - Infrastructure setup
- `.env` - Environment variables (gitignored)
- `.env.example` - Environment template
- `start-all.sh` - Start all containers
- `kafka-*.sh` - Kafka management scripts
