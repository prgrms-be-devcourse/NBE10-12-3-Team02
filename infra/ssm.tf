# 값은 절대 이 저장소의 .tf 파일에 하드코딩하지 않는다.
# 실제 값은 var.prod_env_content로 주입되며, 그 원본은 gitignore 대상인
# terraform.tfvars(로컬)에만 존재한다. 단, Terraform state 파일 자체에는
# SecureString이라도 평문으로 기록되므로(Terraform의 알려진 동작 방식) state
# 파일 보호(원격 백엔드 암호화, 접근 제한 등)는 별도로 신경 써야 한다.
resource "aws_ssm_parameter" "prod_env" {
  name        = "/nbe10-12-3-team02/prod/env"
  type        = "SecureString"
  description = "Production .env for nbe10-12-3-team02 backend"
  value       = var.prod_env_content

  tags = {
    Name = "${var.project_name}-prod-env"
  }
}
