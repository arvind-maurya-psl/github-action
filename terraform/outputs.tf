output "alb_dns_name" {
  description = "Public DNS name of the Application Load Balancer."
  value       = aws_lb.this.dns_name
}

output "application_url" {
  description = "Base URL for the deployed application."
  value       = var.certificate_arn == "" ? "http://${aws_lb.this.dns_name}" : "https://${aws_lb.this.dns_name}"
}

output "health_check_url" {
  description = "Health endpoint used by the ALB target group."
  value       = "${var.certificate_arn == "" ? "http" : "https"}://${aws_lb.this.dns_name}${var.health_check_path}"
}

output "ecs_cluster_name" {
  description = "ECS cluster name."
  value       = aws_ecs_cluster.this.name
}

output "ecs_service_name" {
  description = "ECS service name."
  value       = aws_ecs_service.this.name
}

output "task_definition_family" {
  description = "Task definition family."
  value       = aws_ecs_task_definition.this.family
}

output "task_definition_arn" {
  description = "ARN of the current task definition revision."
  value       = aws_ecs_task_definition.this.arn
}

output "container_name" {
  description = "Container name referenced by the deploy pipeline."
  value       = var.container_name
}

output "container_image" {
  description = "Image currently referenced by the task definition."
  value       = local.container_image
}

output "ecr_repository_url" {
  description = "URL of the pre-existing ECR repository."
  value       = data.aws_ecr_repository.app.repository_url
}

output "log_group_name" {
  description = "CloudWatch Logs group for container output."
  value       = aws_cloudwatch_log_group.app.name
}

output "vpc_id" {
  description = "VPC id."
  value       = aws_vpc.this.id
}

output "public_subnet_ids" {
  description = "Public subnet ids (ALB)."
  value       = aws_subnet.public[*].id
}

output "task_subnet_ids" {
  description = "Subnets used by Fargate tasks."
  value       = local.task_subnet_ids
}
