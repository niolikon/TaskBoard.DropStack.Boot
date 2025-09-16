# TaskBoard.DropStack.Boot
[![Build](https://github.com/niolikon/TaskBoard.DropStack.Boot/actions/workflows/build-and-test.yml/badge.svg)](https://github.com/niolikon/TaskBoard.DropStack.Boot/actions)
[![Package](https://github.com/niolikon/TaskBoard.DropStack.Boot/actions/workflows/publish-release.yml/badge.svg)](https://github.com/niolikon/TaskBoard.DropStack.Boot/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](https://opensource.org/licenses/MIT)

Task Board DropStack (Spring Case Study)

# Overview

📚 **TaskBoard.DropStack.Boot** is a Spring Boot Web API for **document management** with:
- metadata persisted in **MongoDB**,
- binary content stored in **MinIO** (S3-compatible),
- DTOs + Validation, **MapStruct** mappers, and optimistic locking via `@Version`.

This project showcases clean architecture, testability, and integration with an S3-like object storage.

---

## 🚀 Features

- **Multipart Upload**: upload a file to MinIO and create metadata in Mongo **in a single request**.
- **Metadata CRUD**: create/read/update/delete with DTOs and bean validation.
- **Optimistic Locking**: concurrency control via `@Version` (conflicts return HTTP 409).
- **S3/MinIO integration**: AWS SDK v2, metadata enrichment (`etag`, `size`, `content-type`).
- **Actuator**: health and info endpoints.
- **Clean layering**: thin controllers, service layer orchestrates storage + persistence.

---

## 📖 User Stories (selected)

- ✅ Atomic document upload + metadata persistence (with compensation on DB failure).

---

## 🛠️ Getting Started

### Prerequisites

- **Java 17+**
- **Maven 3+**
- **Docker** (recommended for local stack: MongoDB + MinIO + app)

### Quickstart Guide

1. Clone the repository:
   ```bash
   git clone https://github.com/niolikon/TaskBoard.DropStack.Boot.git
   cd TaskBoard.DropStack.Boot
   ```

2. Build:
   ```bash
   mvn clean install
   ```

3. Run locally (without containers) – requires Mongo & MinIO running and proper environment variables:
   ```bash
   mvn spring-boot:run
   ```

## 🐳 Deploy on container

1. Create a `.env` file at the project root:
   ```ini
    KEYCLOAK_DB_PASSWORD=supersecretkeycloak
    KEYCLOAK_ADMIN_PASSWORD=adminpassword
    
    MONGO_DB_NAME=taskboard_dropstack
    MONGO_ROOT_USER=root
    MONGO_ROOT_PASSWORD=change_me_mongo_root_strong
    MONGO_APP_USER=user
    MONGO_APP_PASSWORD=change_me_mongo_user_strong
    
    MINIO_ROOT_USER=minioadmin
    MINIO_ROOT_PASSWORD=change_me_minio_root_strong
    MINIO_BUCKET=taskboard-dropstack-docs
    MINIO_REGION=eu-south-1
    MINIO_APP_ACCESS_KEY=taskboardapp
    MINIO_APP_SECRET_KEY=change_me_minio_app_strong
   ```

2. Build the project:
   ```bash
   mvn clean package
   ```

3. Build the service image:
   ```bash
   docker build -t taskboard-dropstack-boot:latest .
   ```

4. Start the stack:
   ```bash
   docker-compose up -d
   ```

---

## 📬 Feedback

If you have suggestions or improvements, feel free to open an issue or create a pull request. Contributions are welcome!

---

## 📝 License

This project is licensed under the **MIT License**.

---
🚀 **Developed by Simone Andrea Muscas | Niolikon**

