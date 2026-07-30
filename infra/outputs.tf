output "instance_id" {
  description = "생성된 EC2 인스턴스 ID (SSM 대상 지정 시 사용)"
  value       = aws_instance.app.id
}

output "instance_public_ip" {
  description = "EC2 퍼블릭 IP (cd.yaml의 EC2_APP_HOST에 사용)"
  value       = aws_instance.app.public_ip
}

output "security_group_id" {
  value = aws_security_group.app.id
}

output "ecr_registry" {
  description = "ECR 레지스트리 호스트명"
  value       = local.ecr_registry
}

output "ecr_repository_url" {
  description = "cd.yaml에서 docker push 대상으로 사용할 전체 리포지토리 URL"
  value       = aws_ecr_repository.app.repository_url
}

output "ec2_iam_role_arn" {
  value = aws_iam_role.ec2.arn
}

output "github_actions_role_arn" {
  description = "GitHub Actions secrets.AWS_ROLE_ARN에 설정해야 하는 새 배포 역할 ARN (apply 후 반드시 GitHub 시크릿 갱신 필요)"
  value       = aws_iam_role.github_actions.arn
}

output "github_oidc_provider_arn" {
  value = aws_iam_openid_connect_provider.github.arn
}
