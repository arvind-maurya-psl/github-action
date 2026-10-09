aws_region   = "us-east-1"
project_name = "user-management"
environment  = "dev"

# Pre-existing ECR repository (325574368753.dkr.ecr.us-east-1.amazonaws.com/viru/user-management)
ecr_repository_name = "viru/user-management"
image_tag           = "latest"

container_name    = "springboot-container"
container_port    = 8080
health_check_path = "/api/health"

task_cpu      = 512
task_memory   = 1024
desired_count = 2

# Set to false to run tasks in public subnets and skip NAT gateway cost.
enable_nat_gateway = true
single_nat_gateway = true

enable_autoscaling       = true
autoscaling_min_capacity = 2
autoscaling_max_capacity = 6

container_environment = {
  SPRING_PROFILES_ACTIVE = "prod"
}

# Leave empty for HTTP only; set an ACM ARN to enable HTTPS + HTTP redirect.
certificate_arn = ""
