#!/bin/sh
set -eu

# --- Config ---------------------------------------------------------------
# MinIO service address inside the Docker network
MINIO_ADDR="http://taskboard-dropstack-minio-boot:9000"

# --- Helpers --------------------------------------------------------------
retry() {
  # retry <times> <sleep_seconds> <cmd...>
  times="$1"; shift
  sleep_s="$1"; shift
  i=1
  while true; do
    if "$@"; then
      return 0
    fi
    if [ "$i" -ge "$times" ]; then
      echo ">> ERROR: command failed after ${times} attempts: $*" >&2
      return 1
    fi
    i=$((i+1))
    sleep "$sleep_s"
  done
}

# --- 1) Point mc to MinIO (alias) ----------------------------------------
# Small retry to tolerate slow startups even when healthcheck is green.
retry 10 3 mc alias set minio "$MINIO_ADDR" "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD"

# --- 2) Ensure the bucket exists (idempotent) -----------------------------
mc mb -p "minio/${MINIO_BUCKET}" >/dev/null 2>&1 || true
echo ">> Bucket '${MINIO_BUCKET}' is ready"

# --- 3) Create/ensure an app user (non-root) ------------------------------
# If the user already exists, 'add' fails; we ignore to keep it idempotent.
mc admin user add minio "$MINIO_APP_ACCESS_KEY" "$MINIO_APP_SECRET_KEY" >/dev/null 2>&1 || true
echo ">> App user '${MINIO_APP_ACCESS_KEY}' ensured"

# --- 4) Create a bucket-scoped RW policy and attach it to the app user ----
# Generate the policy JSON on the fly to inject ${MINIO_BUCKET}.
POLICY_FILE="/tmp/policy-app-rw.json"
cat > "$POLICY_FILE" <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "BucketLevel",
      "Effect": "Allow",
      "Action": ["s3:ListBucket"],
      "Resource": ["arn:aws:s3:::${MINIO_BUCKET}"]
    },
    {
      "Sid": "ObjectLevel",
      "Effect": "Allow",
      "Action": [
        "s3:GetObject","s3:PutObject","s3:DeleteObject",
        "s3:AbortMultipartUpload","s3:ListBucketMultipartUploads","s3:ListMultipartUploadParts"
      ],
      "Resource": ["arn:aws:s3:::${MINIO_BUCKET}/*"]
    }
  ]
}
EOF

# Create the policy if it does not exist; ignore errors otherwise.
mc admin policy create minio app-rw "$POLICY_FILE" >/dev/null 2>&1 || true
# Attach (or re-attach) the policy to the app user (idempotent).
mc admin policy attach minio app-rw --user "$MINIO_APP_ACCESS_KEY" >/dev/null 2>&1 || true
echo ">> Policy 'app-rw' attached to user '${MINIO_APP_ACCESS_KEY}'"

echo ">> MinIO bootstrap completed successfully"
