variable "aws_region" {
  description = "AWS region for resources"
  type        = string
  default     = "us-west-2"
}

variable "service_name" {
  description = "Name of the warehouse consumer service"
  type        = string
  default     = "warehouse-consumer"
}

variable "ecr_repository_name" {
  description = "Name of the ECR repository"
  type        = string
  default     = "warehouse-consumer"
}

# -----------------------------------------------------------------
# Cluster config — set create_cluster = false if the team already
# provisioned a shared cluster from another Terraform config.
# -----------------------------------------------------------------
variable "create_cluster" {
  description = "Whether to create a new ECS cluster (false = use existing)"
  type        = bool
  default     = true
}

variable "cluster_name" {
  description = "ECS cluster name (created or referenced). Coordinate with team."
  type        = string
  default     = "cs6650-assignment3-cluster"
}

variable "desired_count" {
  description = "Number of warehouse consumer tasks to run"
  type        = number
  default     = 1
}

# -----------------------------------------------------------------
# RabbitMQ connection — the consumer needs to reach the broker.
# Set these to match your RabbitMQ container/service address.
# -----------------------------------------------------------------
variable "rabbitmq_host" {
  description = "RabbitMQ host (private IP or service discovery name)"
  type        = string
}

variable "rabbitmq_port" {
  description = "RabbitMQ AMQP port"
  type        = number
  default     = 5672
}

variable "rabbitmq_user" {
  description = "RabbitMQ username"
  type        = string
  default     = "guest"
}

variable "rabbitmq_pass" {
  description = "RabbitMQ password"
  type        = string
  default     = "guest"
  sensitive   = true
}

# -----------------------------------------------------------------
# Consumer tuning — adjust these for load testing
# -----------------------------------------------------------------
variable "num_consumer_threads" {
  description = "Number of consumer threads in the warehouse"
  type        = number
  default     = 10
}

variable "prefetch_count" {
  description = "RabbitMQ prefetch (basicQos) per consumer thread"
  type        = number
  default     = 10
}

variable "rabbitmq_retry_count" {
  description = "Number of retry attempts when connecting to RabbitMQ"
  type        = number
  default     = 30
}

variable "rabbitmq_retry_wait_s" {
  description = "Seconds to wait between RabbitMQ connection retries"
  type        = number
  default     = 2
}

# -----------------------------------------------------------------
# Task sizing — Fargate CPU/memory
# -----------------------------------------------------------------
variable "task_cpu" {
  description = "Fargate task CPU units (256 = 0.25 vCPU)"
  type        = string
  default     = "512"
}

variable "task_memory" {
  description = "Fargate task memory in MiB"
  type        = string
  default     = "1024"
}
