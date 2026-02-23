# AWS Deployment Instructions

## Prerequisites

- AWS CLI v2 configured with Learner Lab credentials (`~/.aws/credentials`)
- Docker Desktop running
- Terraform installed (`terraform --version`)
- Learner Lab session active (green indicator)

> **Reminder:** Learner Lab credentials expire every 4 hours. Re-copy them from AWS Details → AWS CLI before each session.

---

## Step 1: Provision Infrastructure

```bash
cd terraform

# Initialize Terraform (first time only)
terraform init

# Preview what will be created
terraform plan

# Create resources (type 'yes' when prompted)
terraform apply
```

This creates: ECR repository, ECS cluster, task definition, ECS service, security group, and CloudWatch log group.

> **Team note:** If there is a shared cluster created, edit `terraform.tfvars` first:
> ```hcl
> create_cluster = false
> cluster_name   = "cs6650-assignment3-cluster"  # must match existing cluster
> ```

---

## Step 2: Update RabbitMQ Host

After the RabbitMQ broker is deployed, get its private IP and update `terraform.tfvars`:

```hcl
rabbitmq_host = "10.0.1.42"  # replace with actual RabbitMQ private IP
```

Then re-apply:

```bash
terraform apply
```

---

## Step 3: Build and Push Image

```bash
cd ..  # back to warehouse-consumer root

# Get ECR URL
ECR_URL=$(cd terraform && terraform output -raw ecr_repository_url && cd ..)
ECR_BASE=$(echo "$ECR_URL" | cut -d'/' -f1)

# Login to ECR
aws ecr get-login-password --region us-west-2 | docker login --username AWS --password-stdin "$ECR_BASE"

# Build for Fargate (linux/amd64)
docker build --platform linux/amd64 -t warehouse-consumer:latest .

# Tag and push
docker tag warehouse-consumer:latest "${ECR_URL}:latest"
docker push "${ECR_URL}:latest"
```

---

## Step 4: Deploy

```bash
# Force ECS to pull the new image
aws ecs update-service \
  --cluster cs6650-assignment3-cluster \
  --service warehouse-consumer \
  --force-new-deployment \
  --region us-west-2
```

Or use the convenience script (does Steps 3 + 4 together):

```bash
./deploy.sh
```

---

## Step 5: Verify

```bash
# Tail logs in real time
aws logs tail /ecs/warehouse-consumer --follow --region us-west-2

# Check task status
aws ecs describe-services \
  --cluster cs6650-assignment3-cluster \
  --services warehouse-consumer \
  --region us-west-2 \
  --query 'services[0].{running:runningCount,desired:desiredCount,status:status}'
```

You should see in the logs:
```
Warehouse Consumer starting - threads=10, prefetch=10
Connected to RabbitMQ successfully
All 10 consumer threads started. Waiting for messages...
```

---

## Redeploying After Code Changes

```bash
./deploy.sh   # rebuilds, pushes, and forces new deployment
```

## Redeploying After Config Changes Only

Edit `terraform.tfvars` (e.g., change `num_consumer_threads` or `rabbitmq_retry_count`), then:

```bash
cd terraform
terraform apply
cd ..
aws ecs update-service --cluster cs6650-assignment3-cluster --service warehouse-consumer --force-new-deployment --region us-west-2
```

---

## Capturing Shutdown Screenshot

```bash
# Stop the consumer
aws ecs update-service \
  --cluster cs6650-assignment3-cluster \
  --service warehouse-consumer \
  --desired-count 0 \
  --region us-west-2

# Wait ~30 seconds for graceful shutdown, then check logs
aws logs tail /ecs/warehouse-consumer --region us-west-2 | tail -10
```

Look for:
```
========================================
Total number of orders: 200000
========================================
```

Screenshot this output for the report.

To restart:
```bash
aws ecs update-service \
  --cluster cs6650-assignment3-cluster \
  --service warehouse-consumer \
  --desired-count 1 \
  --region us-west-2
```

---

## Clean Up (End of Day)

```bash
cd terraform
terraform destroy   # type 'yes' when prompted
```

**Always destroy resources when done for the day to conserve Learner Lab credits.**

---

## Troubleshooting

| Problem | Cause | Fix |
|---|---|---|
| Task stays in PROVISIONING | Image not in ECR | Run `./deploy.sh` to push the image |
| Task starts then stops | Can't reach RabbitMQ | Check `rabbitmq_host` in tfvars, verify RabbitMQ is running |
| "CannotPullContainerError" | ECR auth or image missing | Re-push image; check ECR repo exists |
| No logs in CloudWatch | Task crashed before logging | Check ECS task "Stopped reason" in console |
| "Connection refused" in logs | Wrong RabbitMQ host/port | Update `rabbitmq_host` in tfvars, re-apply + redeploy |
| Credentials expired | Learner Lab session timeout | Re-copy credentials from AWS Details → `~/.aws/credentials` |
