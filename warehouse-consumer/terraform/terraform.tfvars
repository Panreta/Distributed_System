aws_region          = "us-west-2"
service_name        = "warehouse-consumer"
ecr_repository_name = "warehouse-consumer"
desired_count       = 1

# -----------------------------------------------------------------
# Cluster — coordinate with team
# Set create_cluster = false if another team member already created it
# -----------------------------------------------------------------
create_cluster = true
cluster_name   = "cs6650-assignment3-cluster"  # TODO: coordinate with team

# -----------------------------------------------------------------
# RabbitMQ connection — MUST be updated before deploying
# Use the private IP or service discovery name of your RabbitMQ container
# -----------------------------------------------------------------
rabbitmq_host = "REPLACE_WITH_RABBITMQ_HOST"  # e.g. "10.0.1.42" or rabbitmq service name
rabbitmq_port = 5672
rabbitmq_user = "guest"
rabbitmq_pass = "guest"

# -----------------------------------------------------------------
# Consumer tuning — adjust during load testing
# -----------------------------------------------------------------
num_consumer_threads = 10
prefetch_count       = 10

# -----------------------------------------------------------------
# Task sizing — increase if consumer is CPU/memory bound
# Valid Fargate combos: 256/512, 512/1024, 1024/2048, etc.
# -----------------------------------------------------------------
task_cpu    = "512"    # 0.5 vCPU
task_memory = "1024"   # 1 GB
