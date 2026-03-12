terraform {
  required_providers {
    aws = { source = "hashicorp/aws", version = "~> 5.0" }
  }
}

provider "aws" { region = var.aws_region }

variable "aws_region"        { default = "us-east-1" }
variable "instance_type"     { default = "t3.micro" }
variable "ami_id"            { description = "Amazon Linux 2023 AMI for your region" }
variable "key_name"          { description = "EC2 key pair name" }
variable "write_quorum_size" { default = "5" }
variable "read_quorum_size"  { default = "1" }
variable "docker_image"      { default = "your-dockerhub/leader-follower-kv:latest" }

data "aws_vpc" "default"     { default = true }
data "aws_subnets" "default" {
  filter { name = "vpc-id", values = [data.aws_vpc.default.id] }
}

locals {
  follower_names = ["follower1", "follower2", "follower3", "follower4"]
  startup_script = <<-SH
    #!/bin/bash
    yum update -y && yum install -y docker
    systemctl start docker && systemctl enable docker
  SH
}

resource "aws_security_group" "alb" {
  name   = "kv-alb-sg"
  vpc_id = data.aws_vpc.default.id
  ingress { from_port = 80,  to_port = 80,  protocol = "tcp", cidr_blocks = ["0.0.0.0/0"] }
  egress  { from_port = 0,   to_port = 0,   protocol = "-1",  cidr_blocks = ["0.0.0.0/0"] }
}

resource "aws_security_group" "nodes" {
  name   = "kv-nodes-sg"
  vpc_id = data.aws_vpc.default.id
  ingress { from_port = 8080, to_port = 8080, protocol = "tcp", self = true }
  ingress { from_port = 8080, to_port = 8080, protocol = "tcp", security_groups = [aws_security_group.alb.id] }
  ingress { from_port = 22,   to_port = 22,   protocol = "tcp", cidr_blocks = ["0.0.0.0/0"] }
  egress  { from_port = 0,    to_port = 0,    protocol = "-1",  cidr_blocks = ["0.0.0.0/0"] }
}

resource "aws_instance" "followers" {
  count                  = 4
  ami                    = var.ami_id
  instance_type          = var.instance_type
  key_name               = var.key_name
  vpc_security_group_ids = [aws_security_group.nodes.id]
  user_data = <<-SH
    ${local.startup_script}
    docker run -d -e ROLE=follower \
      -e WRITE_QUORUM_SIZE=${var.write_quorum_size} \
      -e READ_QUORUM_SIZE=${var.read_quorum_size} \
      -p 8080:8080 ${var.docker_image}
  SH
  tags = { Name = local.follower_names[count.index], Role = "follower" }
}

resource "aws_instance" "leader" {
  ami                    = var.ami_id
  instance_type          = var.instance_type
  key_name               = var.key_name
  vpc_security_group_ids = [aws_security_group.nodes.id]
  depends_on             = [aws_instance.followers]
  user_data = <<-SH
    ${local.startup_script}
    docker run -d -e ROLE=leader \
      -e FOLLOWER_URLS="http://${aws_instance.followers[0].private_ip}:8080,http://${aws_instance.followers[1].private_ip}:8080,http://${aws_instance.followers[2].private_ip}:8080,http://${aws_instance.followers[3].private_ip}:8080" \
      -e WRITE_QUORUM_SIZE=${var.write_quorum_size} \
      -e READ_QUORUM_SIZE=${var.read_quorum_size} \
      -p 8080:8080 ${var.docker_image}
  SH
  tags = { Name = "leader", Role = "leader" }
}

resource "aws_lb" "kv" {
  name               = "kv-alb"
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = data.aws_subnets.default.ids
}

resource "aws_lb_target_group" "nodes" {
  name     = "kv-nodes-tg"
  port     = 8080
  protocol = "HTTP"
  vpc_id   = data.aws_vpc.default.id
  health_check { path = "/actuator/health", healthy_threshold = 2, interval = 30 }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.kv.arn
  port = 80; protocol = "HTTP"
  default_action { type = "forward", target_group_arn = aws_lb_target_group.nodes.arn }
}

resource "aws_lb_target_group_attachment" "followers" {
  count            = 4
  target_group_arn = aws_lb_target_group.nodes.arn
  target_id        = aws_instance.followers[count.index].id
  port             = 8080
}

resource "aws_lb_target_group_attachment" "leader" {
  target_group_arn = aws_lb_target_group.nodes.arn
  target_id        = aws_instance.leader.id
  port             = 8080
}

output "leader_public_ip" { value = aws_instance.leader.public_ip }
output "alb_dns_name"     { value = aws_lb.kv.dns_name }
output "follower_ips"     { value = aws_instance.followers[*].public_ip }