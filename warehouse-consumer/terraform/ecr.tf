# ECR repository for the warehouse consumer image
resource "aws_ecr_repository" "warehouse" {
  name                 = var.ecr_repository_name
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = false
  }

  tags = {
    Name        = var.ecr_repository_name
    Environment = "dev"
  }
}
