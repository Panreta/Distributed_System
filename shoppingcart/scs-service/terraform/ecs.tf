data "aws_iam_role" "lab_role" {
  name = "LabRole"
}

data "aws_ecs_cluster" "existing" {
  cluster_name = var.cluster_name
}

resource "aws_cloudwatch_log_group" "scs_logs" {
  name              = "/ecs/${var.service_name}"
  retention_in_days = 7
  tags              = { Name = "${var.service_name}-logs" }
}

resource "aws_security_group" "scs" {
  name        = "${var.service_name}-sg"
  description = "SCS service security group"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    from_port   = 8081
    to_port     = 8081
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "SCS HTTP"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.service_name}-sg" }
}

resource "aws_ecs_task_definition" "scs" {
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
      image = "${aws_ecr_repository.scs.repository_url}:latest"

      portMappings = [
        { containerPort = 8081, hostPort = 8081, protocol = "tcp" }
      ]

      environment = [
        { name = "CCA_BASE_URL",  value = var.cca_base_url },
        { name = "RMQ_HOST",      value = var.rabbitmq_host },
        { name = "RMQ_PORT",      value = tostring(var.rabbitmq_port) },
        { name = "RMQ_USER",      value = var.rabbitmq_user },
        { name = "RMQ_PASS",      value = var.rabbitmq_pass },
        { name = "RMQ_QUEUE",     value = var.rmq_queue },
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.scs_logs.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "ecs"
        }
      }
    }
  ])
}

resource "aws_ecs_service" "scs" {
  name            = var.service_name
  cluster         = data.aws_ecs_cluster.existing.id
  task_definition = aws_ecs_task_definition.scs.arn
  desired_count   = 1

  capacity_provider_strategy {
    capacity_provider = "FARGATE"
    weight            = 100
    base              = 1
  }

  network_configuration {
    subnets          = data.aws_subnets.default.ids
    security_groups  = [aws_security_group.scs.id]
    assign_public_ip = true
  }

  tags = { Name = var.service_name }
}