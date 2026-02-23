variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-west-2"
}

variable "service_name" {
  description = "Name of the RabbitMQ service"
  type        = string
  default     = "rabbitmq-broker"
}

variable "cluster_name" {
  description = "ECS cluster name"
  type        = string
}

variable "create_cluster" {
  description = "Set true to create cluster, false to reuse existing"
  type        = bool
  default     = true
}

variable "rmq_user" {
  description = "RabbitMQ default username"
  type        = string
  default     = "guest"
}

variable "rmq_pass" {
  description = "RabbitMQ default password"
  type        = string
  default     = "guest"
}

variable "task_cpu" {
  description = "Fargate task CPU units"
  type        = string
  default     = "512"
}

variable "task_memory" {
  description = "Fargate task memory (MB)"
  type        = string
  default     = "1024"
}