output "ecr_repository_url" {
  value = aws_ecr_repository.cca.repository_url
}

output "service_name" {
  value = aws_ecs_service.cca.name
}

output "get_cca_ip" {
  description = "Command to get CCA private IP after deployment"
  value       = <<-EOT
    aws ecs list-tasks --cluster ${var.cluster_name} --service-name ${var.service_name} --region ${var.aws_region} --query 'taskArns[0]' --output text | xargs -I {} aws ecs describe-tasks --cluster ${var.cluster_name} --tasks {} --region ${var.aws_region} --query 'tasks[0].attachments[0].details[?name==`privateIPv4Address`].value' --output text
  EOT
}