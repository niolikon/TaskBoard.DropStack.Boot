#!/usr/bin/env bash
set -euo pipefail

: "${MONGO_DB_NAME:?MONGO_DB_NAME mancante}"
: "${APP_DB_USER:?APP_DB_USER mancante}"
: "${APP_DB_PASSWORD:?APP_DB_PASSWORD mancante}"

echo ">> Mongo init: creating app user '${APP_DB_USER}' on DB '${MONGO_DB_NAME}'"

mongosh --quiet <<EOF
db = db.getSiblingDB("${MONGO_DB_NAME}");

// Create user only if not exists (idempotent)
if (!db.getUser("${APP_DB_USER}")) {
  db.createUser({
    user: "${APP_DB_USER}",
    pwd:  "${APP_DB_PASSWORD}",
    roles: [{ role: "readWrite", db: "${MONGO_DB_NAME}" }]
  });
  print(">> Created user ${APP_DB_USER}@${MONGO_DB_NAME}");
} else {
  print(">> User ${APP_DB_USER}@${MONGO_DB_NAME} already exists");
}

EOF

echo ">> Mongo init: done"
