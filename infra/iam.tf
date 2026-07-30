resource "aws_ecr_repository" "app" {
  name                 = var.ecr_repository_name
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name = "${var.project_name}-ecr"
  }
}

# 무과금 방침: ECR 프리티어(스토리지 500MB/월, 12개월)를 넘지 않도록 오래된 이미지 자동 정리
resource "aws_ecr_lifecycle_policy" "app" {
  repository = aws_ecr_repository.app.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Expire untagged images older than 1 day"
        selection = {
          tagStatus   = "untagged"
          countType   = "sinceImagePushed"
          countUnit   = "days"
          countNumber = 1
        }
        action = {
          type = "expire"
        }
      },
      {
        rulePriority = 2
        description  = "Keep only the last 10 tagged images"
        selection = {
          tagStatus      = "tagged"
          tagPatternList = ["*"]
          countType      = "imageCountMoreThan"
          countNumber    = 10
        }
        action = {
          type = "expire"
        }
      }
    ]
  })
}

data "aws_iam_policy_document" "ec2_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "ec2" {
  name               = "${var.project_name}-ec2-role"
  assume_role_policy = data.aws_iam_policy_document.ec2_assume_role.json

  tags = {
    Name = "${var.project_name}-ec2-role"
  }
}

# 배포가 SSM send-command로 이루어지므로 EC2가 SSM 관리 대상이 되도록 부착
resource "aws_iam_role_policy_attachment" "ssm_core" {
  role       = aws_iam_role.ec2.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

data "aws_iam_policy_document" "ecr_pull" {
  statement {
    sid       = "ECRAuth"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    sid = "ECRPull"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:GetDownloadUrlForLayer",
      "ecr:BatchGetImage",
    ]
    resources = [aws_ecr_repository.app.arn]
  }
}

resource "aws_iam_role_policy" "ecr_pull" {
  name   = "${var.project_name}-ecr-pull"
  role   = aws_iam_role.ec2.id
  policy = data.aws_iam_policy_document.ecr_pull.json
}

resource "aws_iam_instance_profile" "ec2" {
  name = "${var.project_name}-ec2-profile"
  role = aws_iam_role.ec2.name
}

# EC2가 SSM Parameter Store의 운영 .env 값을 조회할 수 있도록 허용 (해당 파라미터로 한정)
data "aws_iam_policy_document" "ssm_get_prod_env" {
  statement {
    sid       = "SSMGetProdEnvParameter"
    actions   = ["ssm:GetParameter"]
    resources = [aws_ssm_parameter.prod_env.arn]
  }
}

resource "aws_iam_role_policy" "ssm_get_prod_env" {
  name   = "${var.project_name}-ssm-get-prod-env"
  role   = aws_iam_role.ec2.id
  policy = data.aws_iam_policy_document.ssm_get_prod_env.json
}

# ── GitHub Actions OIDC Provider ──
# aws iam list-open-id-connect-providers 결과가 비어있어 계정에 아직 등록되지 않은 상태 -> 신규 생성
resource "aws_iam_openid_connect_provider" "github" {
  url            = "https://token.actions.githubusercontent.com"
  client_id_list = ["sts.amazonaws.com"]

  # GitHub Actions OIDC의 공인된 루트 CA 지문(AWS/GitHub 공식 문서에 명시된 고정값).
  # AWS는 2022년부터 OIDC 발급자의 TLS 체인을 자체 검증하므로 이 값 자체가 보안 경계는 아니지만
  # 리소스 생성 시 필수 필드라 표준값을 그대로 사용.
  thumbprint_list = ["6938fd4d98bab03faadb97b34396831e3780aea1"]

  tags = {
    Name = "${var.project_name}-github-oidc"
  }
}

# ── GitHub Actions가 assume할 배포 역할 ──
# 이 레포의 main 브랜치 push/merge 이벤트에서만 assume 가능하도록 sub 클레임을 엄격히 제한
data "aws_iam_policy_document" "github_actions_assume_role" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github.arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    # GitHub OIDC sub claim이 조직/리포지토리 불변 ID를 포함하는 형태로 발급됨
    # (repo 이름 변경/재생성을 통한 trust policy 우회 방지를 위한 GitHub 측 하드닝)
    # repo:prgrms-be-devcourse@88020948/NBE10-12-3-Team02@1313451004:ref:refs/heads/<branch>
    # — CloudTrail의 AssumeRoleWithWebIdentity AccessDenied 이벤트에서 실측 확인한 값
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values = [
        "repo:prgrms-be-devcourse@88020948/NBE10-12-3-Team02@1313451004:ref:refs/heads/main",
      ]
    }
  }
}

resource "aws_iam_role" "github_actions" {
  name               = "${var.project_name}-github-actions-deploy"
  assume_role_policy = data.aws_iam_policy_document.github_actions_assume_role.json

  tags = {
    Name = "${var.project_name}-github-actions-deploy"
  }
}

data "aws_iam_policy_document" "github_actions_permissions" {
  # ECR 로그인 (계정 전체 대상, 리소스 레벨 제약이 불가한 액션)
  statement {
    sid       = "ECRAuth"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  # ECR push/pull (docker/build-push-action, cache 조회 포함)
  statement {
    sid = "ECRPushPull"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:GetDownloadUrlForLayer",
      "ecr:BatchGetImage",
      "ecr:PutImage",
      "ecr:InitiateLayerUpload",
      "ecr:UploadLayerPart",
      "ecr:CompleteLayerUpload",
    ]
    resources = [aws_ecr_repository.app.arn]
  }

  # SSM 배포 (send-command 대상은 이 인스턴스 + AWS-RunShellScript 문서로 한정)
  statement {
    sid     = "SSMSendCommand"
    actions = ["ssm:SendCommand"]
    resources = [
      aws_instance.app.arn,
      "arn:aws:ssm:${var.aws_region}::document/AWS-RunShellScript",
    ]
  }

  # 명령 실행 결과 조회 (리소스 레벨 제약이 불가한 액션)
  statement {
    sid = "SSMCommandStatus"
    actions = [
      "ssm:GetCommandInvocation",
      "ssm:ListCommandInvocations",
    ]
    resources = ["*"]
  }
}

resource "aws_iam_role_policy" "github_actions_permissions" {
  name   = "${var.project_name}-github-actions-permissions"
  role   = aws_iam_role.github_actions.id
  policy = data.aws_iam_policy_document.github_actions_permissions.json
}
