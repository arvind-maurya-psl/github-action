variable "aws_region" {
  description = "AWS region for all resources."
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Short name used as a prefix for every resource."
  type        = string
  default     = "user-management"
}

variable "environment" {
  description = "Deployment environment (dev/stage/prod)."
  type        = string
  default     = "dev"
}

# ---------------------------------------------------------------------------
# Networking
# ---------------------------------------------------------------------------

variable "vpc_cidr" {
  description = "CIDR block for the VPC."
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for the public subnets (one per AZ, minimum 2)."
  type        = list(string)
  default     = ["10.0.0.0/20", "10.0.16.0/20"]
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks for the private subnets (one per AZ, minimum 2)."
  type        = list(string)
  default     = ["10.0.128.0/20", "10.0.144.0/20"]
}

variable "enable_nat_gateway" {
  description = <<-EOT
    true  -> Fargate tasks run in private subnets behind NAT gateways (production shape, extra cost).
    false -> Fargate tasks run in public subnets with a public IP (cheap test shape).
  EOT
  type        = bool
  default     = true
}

variable "single_nat_gateway" {
  description = "Create only one NAT gateway (cheaper) instead of one per AZ."
  type        = bool
  default     = true
}

variable "alb_ingress_cidrs" {
  description = "CIDR blocks allowed to reach the ALB listener."
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

# ---------------------------------------------------------------------------
# Container image (ECR repository is pre-existing and NOT managed here)
# ---------------------------------------------------------------------------

variable "ecr_repository_name" {
  description = "Name of the existing ECR repository holding the application image."
  type        = string
  default     = "viru/user-management"
}

variable "image_tag" {
  description = "Image tag to deploy from the existing ECR repository."
  type        = string
  default     = "latest"
}

# ---------------------------------------------------------------------------
# ECS task / service
# ---------------------------------------------------------------------------

variable "container_name" {
  description = "Container name inside the task definition (referenced by the ALB target group)."
  type        = string
  default     = "springboot-container"
}

variable "container_port" {
  description = "Port the Spring Boot application listens on."
  type        = number
  default     = 8080
}

variable "task_cpu" {
  description = "Fargate task CPU units (256, 512, 1024, ...)."
  type        = number
  default     = 512
}

variable "task_memory" {
  description = "Fargate task memory in MiB. Must be compatible with task_cpu."
  type        = number
  default     = 1024
}

variable "desired_count" {
  description = "Number of tasks the ECS service keeps running."
  type        = number
  default     = 2
}

variable "health_check_path" {
  description = "HTTP path used by the ALB target group health check."
  type        = string
  default     = "/api/health"
}

variable "container_environment" {
  description = "Plain (non-secret) environment variables injected into the container."
  type        = map(string)
  default = {
    SPRING_PROFILES_ACTIVE = "prod"
  }
}

variable "log_retention_in_days" {
  description = "CloudWatch Logs retention for the container log group."
  type        = number
  default     = 30
}

# ---------------------------------------------------------------------------
# Autoscaling
# ---------------------------------------------------------------------------

variable "enable_autoscaling" {
  description = "Enable Application Auto Scaling for the ECS service."
  type        = bool
  default     = true
}

variable "autoscaling_min_capacity" {
  description = "Minimum task count when autoscaling is enabled."
  type        = number
  default     = 2
}

variable "autoscaling_max_capacity" {
  description = "Maximum task count when autoscaling is enabled."
  type        = number
  default     = 6
}

variable "autoscaling_cpu_target" {
  description = "Target average CPU utilisation percentage."
  type        = number
  default     = 70
}

variable "autoscaling_memory_target" {
  description = "Target average memory utilisation percentage."
  type        = number
  default     = 75
}

# ---------------------------------------------------------------------------
# HTTPS (optional)
# ---------------------------------------------------------------------------

variable "certificate_arn" {
  description = "ACM certificate ARN. When set, an HTTPS:443 listener is created and HTTP:80 redirects to it."
  type        = string
  default     = ""
}
