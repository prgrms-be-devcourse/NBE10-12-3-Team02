variable "aws_region" {
  description = "배포 대상 AWS 리전"
  type        = string
  default     = "ap-northeast-2"
}

variable "project_name" {
  description = "리소스 이름 접두사"
  type        = string
  default     = "nbe10-12-3-team02"
}

variable "vpc_cidr" {
  description = "VPC CIDR 블록"
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidr" {
  description = "퍼블릭 서브넷 CIDR 블록"
  type        = string
  default     = "10.0.1.0/24"
}

variable "availability_zone" {
  description = "EC2/서브넷을 배치할 가용 영역"
  type        = string
  default     = "ap-northeast-2a"
}

variable "app_allowed_cidr" {
  description = "백엔드 앱(8080) 접근을 허용할 CIDR. 일반적으로 프론트/외부 클라이언트를 위해 0.0.0.0/0"
  type        = string
  default     = "0.0.0.0/0"
}

variable "instance_type" {
  description = "EC2 인스턴스 타입 (프리티어: t3.micro)"
  type        = string
  default     = "t3.micro"
}

variable "github_repository" {
  description = "OIDC 신뢰 대상 GitHub 레포 (org/repo 형식). main 브랜치에서만 배포 역할을 assume할 수 있도록 trust policy에 사용"
  type        = string
  default     = "prgrms-be-devcourse/NBE10-12-3-Team02"
}

variable "ecr_repository_name" {
  description = "Terraform이 생성할 ECR 리포지토리 이름. AWS ECR은 소문자/숫자/하이픈/언더스코어/슬래시만 허용(대문자 불가) — cd.yaml의 push 대상 태그도 이 값과 동일한 소문자 이름으로 맞춰야 함"
  type        = string
  default     = "nbe10-12-3-team02"
}

variable "mysql_data_volume_size" {
  description = "EC2 루트 EBS 볼륨 크기(GB). MySQL 데이터까지 같은 볼륨에 저장하므로 여유있게 설정 (AWS 프리티어 한도: 월 30GB)"
  type        = number
  default     = 20
}

variable "image_tag" {
  description = "부트스트랩 시점에 최초로 pull할 이미지 태그. 이후 실제 배포 태그는 CD 파이프라인이 갱신"
  type        = string
  default     = "latest"
}
