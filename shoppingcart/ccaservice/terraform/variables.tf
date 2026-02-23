variable "aws_region" {
  type    = string
  default = "us-west-2"
}

variable "service_name" {
  type    = string
  default = "cca-service"
}

variable "cluster_name" {
  type    = string
}

variable "ecr_repository_name" {
  type    = string
  default = "ccaservice"
}

variable "task_cpu" {
  type    = string
  default = "256"
}

variable "task_memory" {
  type    = string
  default = "512"
}