resource "aws_security_group" "app" {
  name        = "${var.project_name}-sg"
  description = "Backend app(8080) access. No SSH inbound - deploy is via SSM."
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "Backend app (Spring Boot)"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = [var.app_allowed_cidr]
  }

  egress {
    description = "Allow all outbound (ECR, RDS, SSM, package repos, etc.)"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-sg"
  }
}
