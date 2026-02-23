variable "aws_region" {
  type    = string
  default = "us-west-2"
}

variable "service_name" {
  type    = string
  default = "scs-service"
}

variable "cluster_name" {
  type    = string
}

variable "ecr_repository_name" {
  type    = string
  default = "scs-service"
}

variable "cca_base_url" {
  description = "Base URL for CCA service e.g. http://10.0.1.x:8080"
  type        = string
}

variable "rabbitmq_host" {
  description = "Private IP of RabbitMQ broker"
  type        = string
}

variable "rabbitmq_port" {
  type    = number
  default = 5672
}

variable "rabbitmq_user" {
  type    = string
  default = "guest"
}

variable "rabbitmq_pass" {
  type    = string
  default = "guest"
}

variable "rmq_queue" {
  type    = string
  default = "orders_queue"
}

variable "task_cpu" {
  type    = string
  default = "512"
}

variable "task_memory" {
  type    = string
  default = "1024"
}