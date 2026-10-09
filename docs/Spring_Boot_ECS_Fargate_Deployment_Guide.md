**Spring Boot Microservice Deployment on AWS ECS Fargate**

Detailed AWS Console Runbook

*Scope: Docker image in Amazon ECR → ECS Fargate → Application Load
Balancer → CloudWatch*

This document is a reusable deployment SOP for a Java Spring Boot
microservice whose Docker image is already built and pushed to Amazon
ECR. It covers every AWS component required for an internet-facing ECS
Fargate deployment, production recommendations, validation,
troubleshooting, and subsequent releases.

# 1. Reference Architecture

> Internet  
> \|  
> Route 53 (optional) + ACM certificate  
> \| HTTPS 443  
> Application Load Balancer \[public subnets, ALB-SG\]  
> \| HTTP 8080  
> Target Group (target type = IP)  
> \|  
> ECS Service  
> \|  
> Fargate Tasks \[private subnets, ECS-SG\]  
> \|  
> ECR (container image) CloudWatch Logs  
> \| IAM execution/task roles  
> +----------------------- Secrets Manager / SSM (optional)

AWS Fargate is the serverless compute layer for ECS: the application is
packaged as a container while AWS manages the underlying compute
capacity. Fargate task definitions use awsvpc networking. \[1\]\[2\]

# 2. Components and Responsibilities

| **Component** | **Purpose** | **Required?** |
|----|----|----|
| VPC | Network boundary for ALB and Fargate tasks | Yes |
| Public subnets | Host internet-facing ALB nodes in multiple Availability Zones | Yes for public ALB |
| Private subnets | Recommended placement for production Fargate tasks | Recommended |
| Internet Gateway | Internet connectivity for public ALB | Yes for public ALB |
| NAT Gateway / VPC endpoints | Outbound AWS/internet access for private tasks | Depends on design |
| Security Groups | Stateful network controls for ALB and tasks | Yes |
| Amazon ECR | Stores the Spring Boot OCI/Docker image | Yes |
| ECS Cluster | Logical grouping for ECS services/tasks | Yes |
| Task Definition | Container image, CPU, memory, ports, IAM roles, logs, environment | Yes |
| ECS Fargate Service | Maintains desired task count and deployments | Yes |
| Target Group | Registers task ENI IPs and performs health checks | Yes with ALB |
| Application Load Balancer | Public HTTP/HTTPS entry point and routing | Usually for web/API |
| IAM Execution Role | Lets ECS agent pull ECR image and publish logs | Yes for this design |
| IAM Task Role | Permissions used by application code | If app calls AWS APIs |
| CloudWatch Logs | Centralized container stdout/stderr | Recommended |
| ACM + Route 53 | TLS certificate and friendly DNS | Production recommended |
| Secrets Manager / SSM | Secure external configuration/secrets | As needed |
| Application Auto Scaling | Scales ECS service task count | Production recommended |

# 3. Prerequisites

- AWS account and IAM identity with permissions to manage ECS, EC2
  networking/ALB, IAM roles, ECR and CloudWatch.

- Docker image pushed to ECR, preferably with an immutable/versioned tag
  such as 1.0.0 rather than only latest.

- Spring Boot container listens on the expected application port. This
  guide uses 8080.

- A health endpoint that returns HTTP 200, for example /actuator/health.

- VPC and at least two Availability Zones available for resilient ALB
  placement.

# 4. Naming and Example Values

| **Item**           | **Example**          |
|--------------------|----------------------|
| Region             | ap-south-1           |
| VPC                | app-vpc              |
| ECS cluster        | springboot-cluster   |
| ECS service        | springboot-service   |
| Task family        | springboot-task      |
| Container          | springboot-container |
| Container port     | 8080                 |
| ALB                | springboot-alb       |
| Target group       | springboot-tg        |
| ALB security group | springboot-alb-sg    |
| ECS security group | springboot-ecs-sg    |
| Execution role     | ecsTaskExecutionRole |
| Log group          | /ecs/springboot-app  |

# 5. Step-by-Step Provisioning in AWS Console

## 5.1. Verify the ECR image

Open Amazon ECR → Repositories → select the repository → Images. Copy
the complete image URI including the tag. Confirm that the required
architecture (x86_64 or ARM64) matches the task definition you will
create.

> 123456789012.dkr.ecr.ap-south-1.amazonaws.com/my-spring-app:1.0.0

## 5.2. Prepare the VPC and subnets

Use one VPC for the ALB and ECS service. For production, place the
internet-facing ALB in public subnets and Fargate tasks in private
subnets. Select at least two Availability Zones for the ALB. Ensure the
ALB has every Availability Zone enabled in which ECS can place a target;
otherwise a target can report “Target is in an Availability Zone that is
not enabled for the load balancer.” \[3\]

## 5.3. Create the ALB security group

EC2 → Security Groups → Create security group. Use springboot-alb-sg in
the application VPC. Allow inbound 80 for initial HTTP testing and/or
443 for HTTPS from approved client CIDRs or 0.0.0.0/0 for a public
service. Keep outbound connectivity to the ECS task port. AWS recommends
configuring ALB security groups so the load balancer can reach targets
on both listener/health-check ports. \[4\]

## 5.4. Create the ECS task security group

Create springboot-ecs-sg in the same VPC. Add inbound Custom TCP 8080
with source = springboot-alb-sg. Do not expose port 8080 to 0.0.0.0/0.
AWS recommends restricting targets so they accept application traffic
from the load balancer security group. \[4\]

## 5.5. Create or verify the ECS task execution role

IAM → Roles. Look for ecsTaskExecutionRole. The execution role is used
by ECS/Fargate infrastructure, not your application business logic.
Attach the AWS-managed AmazonECSTaskExecutionRolePolicy for the common
ECR and CloudWatch execution needs. If the task definition directly
references Secrets Manager or SSM secrets, add only the additional
permissions required.

## 5.6. Create an application Task Role if required

If Spring Boot calls S3, DynamoDB, SQS, Secrets Manager, etc., create a
separate ECS Task Role with least-privilege permissions. In the task
definition, Task Role is for the container/application while Execution
Role is for ECS agent activities. The task definition supports both
roles separately. \[1\]

## 5.7. Create the ECS cluster

Amazon ECS → Clusters → Create cluster. Name it springboot-cluster. Use
Fargate/serverless capacity. Optionally enable Container Insights. No
EC2 instances are required because Fargate provides the compute layer.
\[2\]

## 5.8. Create the Task Definition

Amazon ECS → Task definitions → Create new task definition. Set family
springboot-task, launch compatibility FARGATE, Linux, CPU architecture
matching the image, awsvpc networking, CPU/memory sized for the JVM, and
execution role ecsTaskExecutionRole. Fargate requires awsvpc network
mode. \[1\]

> Suggested starting point for a small service:  
> CPU: 0.5 vCPU  
> Memory: 1 GB  
> Container name: springboot-container  
> Image: \<ECR image URI\>  
> Container port: 8080/TCP

Under container logging, select awslogs and use /ecs/springboot-app with
a stream prefix such as ecs. Add non-sensitive runtime variables such as
SPRING_PROFILES_ACTIVE=prod. Store credentials/tokens in Secrets Manager
or SSM rather than plain environment values.

## 5.9. Create the Target Group

EC2 → Target Groups → Create target group. Choose target type IP,
protocol HTTP, port 8080, and the same VPC. For ECS services using
awsvpc, AWS requires the target type to be IP because each task has an
elastic network interface. \[3\]

> Health check protocol: HTTP  
> Health check port: traffic port  
> Health check path: /actuator/health  
> Expected response: HTTP 200

## 5.10. Create the Application Load Balancer

EC2 → Load Balancers → Create → Application Load Balancer. Choose
Internet-facing for a public API, the application VPC, at least two
public subnets in different AZs, and springboot-alb-sg. Add an HTTP :80
listener for initial testing that forwards to springboot-tg. For
production add HTTPS :443 with ACM and redirect HTTP to HTTPS.

## 5.11. Create the ECS Fargate Service

ECS → Clusters → springboot-cluster → Services → Create. Select
springboot-task and its desired revision, use Fargate, service name
springboot-service, and set desired task count. Select the same VPC and
appropriate ECS subnets. Attach springboot-ecs-sg. Under Load balancing
choose Application Load Balancer, springboot-container:8080 and
springboot-tg. Enable deployment failure detection/circuit breaker and
rollback where appropriate.

## 5.12. Public IP decision

For a quick test deployment in public subnets, public IP assignment can
be enabled. For production, prefer private task subnets. Private tasks
need a valid outbound design for ECR image pulls, logs, required AWS
APIs, and external dependencies, commonly via NAT and/or appropriate VPC
endpoints.

## 5.13. Wait for service stabilization

Open ECS service → Deployments, Tasks and Events. Desired count and
running count should converge. A service keeps the configured number of
tasks running and registers eligible tasks in the load balancer target
group.

## 5.14. Validate target health

EC2 → Target Groups → springboot-tg → Targets. Confirm the target IP and
port show Healthy. If the target is in an AZ not enabled on the ALB,
edit ALB Network mapping and enable a subnet in that AZ, or restrict ECS
to subnets whose AZs are already enabled on the ALB. AWS requires ALB
subnet coverage for the AZs containing ECS targets. \[3\]

## 5.15. Validate CloudWatch logs

CloudWatch → Log groups → /ecs/springboot-app. Confirm normal Spring
Boot startup and check for database, memory, secrets or port errors.
Logging container stdout/stderr through awslogs is the standard Fargate
task-definition pattern. \[5\]

## 5.16. Test via the ALB

EC2 → Load Balancers → springboot-alb → copy DNS name. Call the
application or health endpoint. Do not test the task ENI directly as
your normal production access path.

> http://\<alb-dns-name\>/actuator/health  
> http://\<alb-dns-name\>/\<application-api-path\>

# 6. HTTPS and DNS for Production

- Request/import a TLS certificate in AWS Certificate Manager for the
  application hostname.

- Add an ALB HTTPS :443 listener using the ACM certificate.

- Forward HTTPS to the existing target group. TLS can terminate at the
  ALB while the ALB forwards HTTP to port 8080 inside the VPC.

- Configure the HTTP :80 listener to redirect to HTTPS :443.

- In Route 53, create an Alias A/AAAA record (as applicable) for the
  application hostname pointing to the ALB.

# 7. Request Flow

> Client → DNS → ALB :443 → Listener rule → Target Group → Fargate ENI
> :8080 → Spring Boot  
> \|  
> +→ health check /actuator/health

# 8. Deployment and Release Process

- Build and test the Spring Boot application.

- Build the Docker image.

- Tag the image with a unique release/version or commit identifier.

- Push the image to the ECR repository.

- Create a new ECS task definition revision that references the new
  image tag.

- Update springboot-service to the new task definition revision.

- ECS performs the deployment and the target group health check gates
  healthy traffic.

- Watch ECS service events, target health and CloudWatch logs.

- If the deployment fails, correct the problem or roll the service back
  to the previous task definition revision.

# 9. Autoscaling and High Availability

For production, run at least two tasks across multiple Availability
Zones where business availability requirements justify it. Configure ECS
Service Auto Scaling using target tracking against CPU, memory or
ALB-related demand signals appropriate to the workload. Fargate
integrates with AWS Application Auto Scaling. \[2\]

# 10. Security Baseline

| **Area** | **Recommended control** |
|----|----|
| Network | Expose ALB only; allow ECS container port from ALB-SG only. |
| IAM | Least-privilege Task Role; separate Execution Role and Task Role. |
| Secrets | Use Secrets Manager/SSM, not passwords committed in image or source. |
| TLS | HTTPS 443 using ACM; redirect HTTP to HTTPS. |
| Images | Pin/version releases; scan images in ECR and patch base images. |
| Tasks | Use private subnets for production where practical. |
| Logging | Centralize application/container logs in CloudWatch. |
| Database | Keep database private; DB SG should permit database port from ECS-SG only. |

# 11. Troubleshooting Matrix

| **Symptom** | **Check / Resolution** |
|----|----|
| Task stays PENDING | Capacity/networking, subnet IP availability, task CPU/memory and ECS events. |
| Task stops immediately | Open stopped reason and CloudWatch logs; check entrypoint, JVM startup, image architecture and memory. |
| CannotPullContainerError | Verify ECR image URI/tag, task execution role, and network path needed to reach ECR. |
| Target: AZ not enabled | Enable that AZ/subnet on ALB Network mapping or remove that AZ subnet from ECS service. \[3\] |
| Target unhealthy / timeout | Verify Spring Boot listens on 8080, ECS-SG allows 8080 from ALB-SG, ALB outbound is allowed, and health path responds. |
| Target unhealthy 404/401 | Use a public health endpoint that returns the expected success code; verify Spring Security rules. |
| ALB 502 | Check task is healthy, app has not crashed, correct port/protocol and application logs. |
| ALB 503 | Confirm healthy registered targets and listener rule points to the correct target group. |
| App cannot reach DB | Check route, DB endpoint, DNS, ECS outbound and DB-SG inbound from ECS-SG. |
| Secrets access denied | Check Task Execution Role if injected by ECS; check Task Role if the application itself calls the secret API. |

# 12. Verification Checklist

- [ ] ECR image exists with expected version tag.

- [ ] Task definition uses FARGATE + awsvpc and correct CPU/memory.

- [ ] Container port equals Spring Boot server port.

- [ ] Execution Role is configured; Task Role is least privilege when
  needed.

- [ ] ALB and ECS resources use the correct VPC.

- [ ] ALB spans the ECS target Availability Zones.

- [ ] Target group type is IP.

- [ ] ECS-SG port 8080 source is ALB-SG, not the internet.

- [ ] Health endpoint returns success.

- [ ] ECS task is RUNNING and service is stable.

- [ ] Target group reports Healthy.

- [ ] CloudWatch logs show successful application startup.

- [ ] ALB DNS/API test succeeds.

- [ ] Production URL uses HTTPS and approved certificate/domain.

- [ ] Monitoring, alarms, scaling and rollback process are defined.

# 13. Component Dependency Map

> ECR image  
> ↓  
> Task Definition ← Execution Role / Task Role / CloudWatch config  
> ↓  
> ECS Cluster → ECS Service → Fargate Task ENIs  
> ↑  
> ALB → Listener → Target Group (IP)  
> ↑ ↑  
> ALB-SG ECS-SG  
> ↑  
> Route 53 + ACM (production)

# 14. References

**\[1\] AWS - Amazon ECS task definition parameters for Fargate**  
https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task_definition_parameters.html

**\[2\] AWS - Architect for AWS Fargate for Amazon ECS**  
https://docs.aws.amazon.com/AmazonECS/latest/developerguide/AWS_Fargate.html

**\[3\] AWS - Use an Application Load Balancer for Amazon ECS**  
https://docs.aws.amazon.com/AmazonECS/latest/developerguide/alb.html

**\[4\] AWS - Security groups for your Application Load Balancer**  
https://docs.aws.amazon.com/elasticloadbalancing/latest/application/load-balancer-update-security-groups.html

**\[5\] AWS - Example Amazon ECS task definitions**  
https://docs.amazonaws.cn/en_us/AmazonECS/latest/developerguide/example_task_definitions.html

Document note: console labels can evolve over time. The logical
resources and relationships in this runbook remain the key validation
points.
