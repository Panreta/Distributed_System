#!/bin/bash
# deploy.sh — Build, push, and deploy the warehouse consumer
# Usage: ./deploy.sh
#
# Prerequisites:
#   1. AWS CLI configured with valid Learner Lab credentials
#   2. Docker running
#   3. terraform init already run in ./terraform/

set -euo pipefail

REGION="us-west-2"

echo "=== Step 1: Get ECR URL from Terraform ==="
cd terraform
ECR_URL=$(terraform output -raw ecr_repository_url)
echo "ECR URL: $ECR_URL"

ECR_BASE=$(echo "$ECR_URL" | cut -d'/' -f1)
CLUSTER=$(terraform output -raw cluster_name)
SERVICE=$(terraform output -raw service_name)
cd ..

echo ""
echo "=== Step 2: Authenticate Docker to ECR ==="
aws ecr get-login-password --region "$REGION" | docker login --username AWS --password-stdin "$ECR_BASE"

echo ""
echo "=== Step 3: Build Docker image (linux/amd64 for Fargate) ==="
docker build --platform linux/amd64 -t warehouse-consumer:latest .

echo ""
echo "=== Step 4: Tag and push to ECR ==="
docker tag warehouse-consumer:latest "${ECR_URL}:latest"
docker push "${ECR_URL}:latest"

echo ""
echo "=== Step 5: Force new deployment ==="
aws ecs update-service \
  --cluster "$CLUSTER" \
  --service "$SERVICE" \
  --force-new-deployment \
  --region "$REGION" \
  --no-cli-pager

echo ""
echo "=== Done! ==="
echo "View logs:  aws logs tail /ecs/warehouse-consumer --follow --region $REGION"
echo "Stop task:  aws ecs update-service --cluster $CLUSTER --service $SERVICE --desired-count 0 --region $REGION"
