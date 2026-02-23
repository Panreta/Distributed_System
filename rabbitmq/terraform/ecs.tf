data "aws_iam_role" "lab_role" {
  name = "LabRole"
}

# -----------------------------------------------------------------
# ECS Cluster — shared across all services
# -----------------------------------------------------------------
resource "aws_ecs_cluster" "main" {
  count = var.create_cluster ? 1 : 0
  name  = var.cluster_name
  tags  = { Name = var.cluster_name }
}

data "aws_ecs_cluster" "existing" {
  count        = var.create_cluster ? 0 : 1
  cluster_name = var.cluster_name
}

locals {
  cluster_id = var.create_cluster ? aws_ecs_cluster.main[0].id : data.aws_ecs_cluster.existing[0].id
}

# -----------------------------------------------------------------
# CloudWatch Log Group
# -----------------------------------------------------------------
resource "aws_cloudwatch_log_group" "rmq_logs" {
  name              = "/ecs/${var.service_name}"
  retention_in_days = 7
  tags              = { Name = "${var.service_name}-logs" }
}

# -----------------------------------------------------------------
# Security Group
# RabbitMQ needs inbound 5672 (AMQP) and 15672 (management UI)
# -----------------------------------------------------------------
resource "aws_security_group" "rmq" {
  name        = "${var.service_name}-sg"
  description = "RabbitMQ broker security group"
  vpc_id      = data.aws_vpc.default.id

  # AMQP - for SCS and Warehouse to connect
  ingress {
    from_port   = 5672
    to_port     = 5672
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "AMQP"
  }

  # Management UI - for your browser
  ingress {
    from_port   = 15672
    to_port     = 15672
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "RabbitMQ Management UI"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.service_name}-sg" }
}

# -----------------------------------------------------------------
# ECS Task Definition
# Uses the official rabbitmq:3-management image — no ECR needed!
# -----------------------------------------------------------------
resource "aws_ecs_task_definition" "rmq" {
  family                   = "${var.service_name}-task"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.task_cpu
  memory                   = var.task_memory
  execution_role_arn       = data.aws_iam_role.lab_role.arn
  task_role_arn            = data.aws_iam_role.lab_role.arn

  container_definitions = jsonencode([
    {
      name  = "${var.service_name}-container"
      image = "rabbitmq:3-management"

      portMappings = [
        { containerPort = 5672,  hostPort = 5672,  protocol = "tcp" },
        { containerPort = 15672, hostPort = 15672, protocol = "tcp" }
      ]

      environment = [
        { name = "RABBITMQ_DEFAULT_USER", value = var.rmq_user },
        { name = "RABBITMQ_DEFAULT_PASS", value = var.rmq_pass }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.rmq_logs.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "ecs"
        }
      }
    }
  ])
}

# -----------------------------------------------------------------
# ECS Service
# -----------------------------------------------------------------
resource "aws_ecs_service" "rmq" {
  name            = var.service_name
  cluster         = local.cluster_id
  task_definition = aws_ecs_task_definition.rmq.arn
  desired_count   = 1

  capacity_provider_strategy {
    capacity_provider = "FARGATE"
    weight            = 100
    base              = 1
  }

  network_configuration {
    subnets          = data.aws_subnets.default.ids
    security_groups  = [aws_security_group.rmq.id]
    assign_public_ip = true
  }

  tags = { Name = var.service_name }
}