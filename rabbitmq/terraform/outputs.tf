output "cluster_name" {
  description = "ECS cluster name — use this in other services"
  value       = var.cluster_name
}

output "service_name" {
  value = aws_ecs_service.rmq.name
}

output "security_group_id" {
  description = "RabbitMQ security group ID"
  value       = aws_security_group.rmq.id
}

output "get_rabbitmq_ip" {
  description = "Command to get RabbitMQ private IP after deployment"
  value       = <<-EOT
    aws ecs list-tasks --cluster ${var.cluster_name} --service-name ${var.service_name} --region ${var.aws_region} --query 'taskArns[0]' --output text | xargs -I {} aws ecs describe-tasks --cluster ${var.cluster_name} --tasks {} --region ${var.aws_region} --query 'tasks[0].attachments[0].details[?name==`privateIPv4Address`].value' --output text
  EOT
}