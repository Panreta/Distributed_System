# -----------------------------------------------------------------
# ECS Cluster — create or reference an existing shared cluster
# -----------------------------------------------------------------
resource "aws_ecs_cluster" "main" {
  count = var.create_cluster ? 1 : 0
  name  = var.cluster_name

  tags = {
    Name        = var.cluster_name
    Environment = "dev"
  }
}

# Reference existing cluster when create_cluster = false
data "aws_ecs_cluster" "existing" {
  count        = var.create_cluster ? 0 : 1
  cluster_name = var.cluster_name
}

locals {
  cluster_id = var.create_cluster ? aws_ecs_cluster.main[0].id : data.aws_ecs_cluster.existing[0].id
}

# -----------------------------------------------------------------
# IAM — use LabRole (pre-created in Learner Lab)
# -----------------------------------------------------------------
data "aws_iam_role" "lab_role" {
  name = "LabRole"
}

# -----------------------------------------------------------------
# CloudWatch Log Group
# -----------------------------------------------------------------
resource "aws_cloudwatch_log_group" "warehouse_logs" {
  name              = "/ecs/${var.service_name}"
  retention_in_days = 7

  tags = {
    Name        = "${var.service_name}-logs"
    Environment = "dev"
  }
}

# -----------------------------------------------------------------
# Security Group
# The consumer only needs OUTBOUND access:
#   - Port 5672 to RabbitMQ (AMQP)
#   - Port 443 for ECR image pull
# No inbound rules needed — it does not serve HTTP traffic.
# -----------------------------------------------------------------
resource "aws_security_group" "warehouse" {
  name        = "${var.service_name}-sg"
  description = "Security group for ${var.service_name} (outbound only)"
  vpc_id      = data.aws_vpc.default.id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Allow all outbound (RabbitMQ, ECR, CloudWatch)"
  }

  tags = {
    Name        = "${var.service_name}-sg"
    Environment = "dev"
  }
}

# -----------------------------------------------------------------
# ECS Task Definition
# -----------------------------------------------------------------
resource "aws_ecs_task_definition" "warehouse" {
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
      image = "${aws_ecr_repository.warehouse.repository_url}:latest"

      # No portMappings — this is a consumer, not a web service

      environment = [
        { name = "RABBITMQ_HOST", value = var.rabbitmq_host },
        { name = "RABBITMQ_PORT", value = tostring(var.rabbitmq_port) },
        { name = "RABBITMQ_USER", value = var.rabbitmq_user },
        { name = "RABBITMQ_PASS", value = var.rabbitmq_pass },
        { name = "NUM_CONSUMER_THREADS", value = tostring(var.num_consumer_threads) },
        { name = "PREFETCH_COUNT", value = tostring(var.prefetch_count) },
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.warehouse_logs.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "ecs"
        }
      }
    }
  ])

  tags = {
    Name        = "${var.service_name}-task"
    Environment = "dev"
  }
}

# -----------------------------------------------------------------
# ECS Service
# -----------------------------------------------------------------
resource "aws_ecs_service" "warehouse" {
  name            = var.service_name
  cluster         = local.cluster_id
  task_definition = aws_ecs_task_definition.warehouse.arn
  desired_count   = var.desired_count

  capacity_provider_strategy {
    capacity_provider = "FARGATE_SPOT"
    weight            = 100
    base              = 0
  }

  capacity_provider_strategy {
    capacity_provider = "FARGATE"
    weight            = 0
    base              = 0
  }

  network_configuration {
    subnets          = data.aws_subnets.default.ids
    security_groups  = [aws_security_group.warehouse.id]
    # Public IP needed so Fargate can pull image from ECR
    # (Learner Lab VPC has no NAT gateway)
    assign_public_ip = true
  }

  tags = {
    Name        = var.service_name
    Environment = "dev"
  }
}
