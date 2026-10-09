resource "aws_security_group" "alb" {
  name        = "${local.name}-alb-sg"
  description = "Public entry point for the ${local.name} Application Load Balancer"
  vpc_id      = aws_vpc.this.id

  tags = { Name = "${local.name}-alb-sg" }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_vpc_security_group_ingress_rule" "alb_http" {
  count = length(var.alb_ingress_cidrs)

  security_group_id = aws_security_group.alb.id
  description       = "HTTP from approved clients"
  cidr_ipv4         = var.alb_ingress_cidrs[count.index]
  from_port         = 80
  to_port           = 80
  ip_protocol       = "tcp"
}

resource "aws_vpc_security_group_ingress_rule" "alb_https" {
  count = var.certificate_arn == "" ? 0 : length(var.alb_ingress_cidrs)

  security_group_id = aws_security_group.alb.id
  description       = "HTTPS from approved clients"
  cidr_ipv4         = var.alb_ingress_cidrs[count.index]
  from_port         = 443
  to_port           = 443
  ip_protocol       = "tcp"
}

resource "aws_vpc_security_group_egress_rule" "alb_to_tasks" {
  security_group_id            = aws_security_group.alb.id
  description                  = "Forward traffic and health checks to ECS tasks"
  referenced_security_group_id = aws_security_group.ecs_tasks.id
  from_port                    = var.container_port
  to_port                      = var.container_port
  ip_protocol                  = "tcp"
}

resource "aws_security_group" "ecs_tasks" {
  name        = "${local.name}-ecs-sg"
  description = "Fargate tasks for ${local.name}"
  vpc_id      = aws_vpc.this.id

  tags = { Name = "${local.name}-ecs-sg" }

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_vpc_security_group_ingress_rule" "tasks_from_alb" {
  security_group_id            = aws_security_group.ecs_tasks.id
  description                  = "Application port, only from the ALB"
  referenced_security_group_id = aws_security_group.alb.id
  from_port                    = var.container_port
  to_port                      = var.container_port
  ip_protocol                  = "tcp"
}

# Outbound is required for ECR image pulls and CloudWatch Logs.
resource "aws_vpc_security_group_egress_rule" "tasks_egress" {
  security_group_id = aws_security_group.ecs_tasks.id
  description       = "Outbound to AWS APIs and dependencies"
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}
