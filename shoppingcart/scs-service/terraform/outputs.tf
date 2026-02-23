output "ecr_repository_url" {
  value = aws_ecr_repository.scs.repository_url
}

output "service_name" {
  value = aws_ecs_service.scs.name
}

output "get_scs_ip" {
  description = "Command to get SCS private IP after deployment"
  value       = <<-EOT
    aws ecs list-tasks --cluster ${var.cluster_name} --service-name ${var.service_name} --region ${var.aws_region} --query 'taskArns[0]' --output text | xargs -I {} aws ecs describe-tasks --cluster ${var.cluster_name} --tasks {} --region ${var.aws_region} --query 'tasks[0].attachments[0].details[?name==`privateIPv4Address`].value' --output text
  EOT
}