# ECS Fargate infrastructure (Terraform)

Recreates, as code, the stack described in [docs/Spring_Boot_ECS_Fargate_Deployment_Guide.md](../docs/Spring_Boot_ECS_Fargate_Deployment_Guide.md).

**The ECR repository is not managed here.** `viru/user-management`
(`325574368753.dkr.ecr.us-east-1.amazonaws.com/viru/user-management`) is read via a
data source, so `terraform destroy` never touches the repository or its images.

## What gets created

| File | Resources |
| --- | --- |
| network.tf | VPC, 2 public + 2 private subnets, IGW, NAT gateway(s), route tables |
| security_groups.tf | ALB SG (80/443 from internet), ECS SG (8080 from ALB SG only) |
| iam.tf | Task execution role, task role, CloudWatch log group `/ecs/user-management-dev` |
| alb.tf | ALB, IP-type target group (health check `/api/health`), HTTP listener (+ HTTPS when `certificate_arn` is set) |
| ecs.tf | Cluster (Container Insights), Fargate task definition, service with circuit breaker + rollback |
| autoscaling.tf | CPU/memory target tracking, unhealthy-target and ALB 5xx alarms |

## Before the first run

1. Destroy the console-built infrastructure (service → cluster → ALB → target group → SGs → task definitions). Leave the ECR repository in place.
2. Create the remote state bucket once:
   ```powershell
   cd terraform/bootstrap
   terraform init
   terraform apply -var="state_bucket_name=<globally-unique-bucket>"
   ```
3. Repository secrets / variables:
   - `AWS_ROLE_ARN` — OIDC role assumed by GitHub Actions. It now needs EC2/VPC, ELBv2, ECS, IAM role, Application Auto Scaling, CloudWatch and S3 state-bucket permissions.
   - `TF_STATE_BUCKET` — bucket created above.
   - `TF_STATE_KEY` (repository *variable*, optional) — defaults to `user-management/ecs-fargate/dev.tfstate`.

## Local usage

```powershell
cd terraform
terraform init -backend-config=backend.hcl   # copy from backend.hcl.example
terraform plan
terraform apply
terraform output application_url
```

`terraform.tfvars` holds the defaults. Set `enable_nat_gateway = false` to run tasks in public
subnets and avoid NAT gateway charges in a test environment.

## Pipelines

- [.github/workflows/terraform.yml](../.github/workflows/terraform.yml) — plans on PR and on pushes touching `terraform/**`; `workflow_dispatch` exposes `plan` / `apply` / `destroy` plus an `image_tag` input. Add `environment: aws-infra` to the job to require manual approval for apply/destroy.
- [.github/workflows/deploy.yml](../.github/workflows/deploy.yml) — builds the JAR, builds and pushes the image to the existing ECR repository, registers a new task-definition revision with that image and rolls the ECS service forward, waiting for stability.

Terraform creates the initial task definition; afterwards the deploy pipeline owns the image, so
`aws_ecs_service` ignores changes to `task_definition` and `desired_count`.

## Resource names used by the deploy pipeline

With `project_name = user-management` and `environment = dev`:

- cluster `user-management-dev-cluster`
- service `user-management-dev-service`
- task family `user-management-dev-task`
- container `springboot-container`

Change these in `terraform.tfvars` and the `env:` block of `deploy.yml` together.
