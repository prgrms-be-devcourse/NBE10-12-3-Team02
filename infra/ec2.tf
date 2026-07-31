data "aws_ssm_parameter" "al2023_ami" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
}

locals {
  ecr_registry = split("/", aws_ecr_repository.app.repository_url)[0]
}

resource "aws_instance" "app" {
  ami                         = data.aws_ssm_parameter.al2023_ami.value
  instance_type               = var.instance_type
  subnet_id                   = aws_subnet.public.id
  vpc_security_group_ids      = [aws_security_group.app.id]
  iam_instance_profile        = aws_iam_instance_profile.ec2.name
  associate_public_ip_address = true

  root_block_device {
    volume_type = "gp3"
    volume_size = var.mysql_data_volume_size
    encrypted   = true
  }

  user_data = templatefile("${path.module}/templates/user_data.sh.tftpl", {
    letsencrypt_email      = var.letsencrypt_email
    docker_compose_content = file("${path.module}/templates/docker-compose.yaml")
    env_file_content       = file("${path.module}/templates/env.example")
  })

  tags = {
    Name = "${var.project_name}-app"
  }
}
