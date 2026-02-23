output "ecr_repository_url" {
  description = "ECR repository URL for the warehouse consumer image"
  value       = aws_ecr_repository.warehouse.repository_url
}

output "cluster_name" {
  description = "Name of the ECS cluster"
  value       = var.cluster_name
}

output "service_name" {
  description = "Name of the ECS service"
  value       = aws_ecs_service.warehouse.name
}

output "log_group" {
  description = "CloudWatch log group for viewing consumer output"
  value       = aws_cloudwatch_log_group.warehouse_logs.name
}

output "view_logs_command" {
  description = "AWS CLI command to tail the warehouse consumer logs"
  value       = <<-EOT
    aws logs tail "/ecs/${var.service_name}" --follow --region ${var.aws_region}
  EOT
}
