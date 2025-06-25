# Provider configuration
provider "aws" {
  region = var.aws_region
}

# Data source for availability zones
data "aws_availability_zones" "available" {
  state = "available"
}

# VPC
resource "aws_vpc" "csye-6225-vpc" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = var.vpc_name
  }
}


# Public Subnets
resource "aws_subnet" "public" {
  count                   = min(3, length(data.aws_availability_zones.available.names))
  vpc_id                  = aws_vpc.csye-6225-vpc.id
  cidr_block              = cidrsubnet(var.vpc_cidr, 8, count.index * 2)
  availability_zone       = data.aws_availability_zones.available.names[count.index]
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.public_subnet_name}-${count.index + 1}"
  }
}

# Private Subnets
resource "aws_subnet" "private" {
  count             = 3
  vpc_id            = aws_vpc.csye-6225-vpc.id
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, count.index * 2 + 1)
  availability_zone = data.aws_availability_zones.available.names[count.index]

  tags = {
    Name = "${var.private_subnet_name}-${count.index + 1}"
  }
}

# Public Route Table
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.csye-6225-vpc.id

  route {
    cidr_block = var.route_table_cidr
    gateway_id = aws_internet_gateway.main.id
  }

  tags = {
    Name = var.public_route_table_name
  }
}

# Private Route Table
resource "aws_route_table" "private" {
  vpc_id = aws_vpc.csye-6225-vpc.id

  tags = {
    Name = var.private_route_table_name
  }
}

# Route Table Associations
resource "aws_route_table_association" "public" {
  count          = min(3, length(data.aws_availability_zones.available.names))
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

# Associate Private Subnets with Private Route Table
resource "aws_route_table_association" "private" {
  count          = min(3, length(data.aws_availability_zones.available.names))
  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private.id
}

# Route for Private Subnets to Use NAT Gateway
resource "aws_route" "private_internet_access" {
  route_table_id         = aws_route_table.private.id
  destination_cidr_block = "0.0.0.0/0"
  nat_gateway_id         = aws_nat_gateway.nat.id
}

# Load Balancer Security Group
resource "aws_security_group" "load_balancer" {
  name        = "load-balancer-security-group"
  description = "Security group for Load Balancer"
  vpc_id      = aws_vpc.csye-6225-vpc.id

  ingress {
    from_port        = 80
    to_port          = 80
    protocol         = "tcp"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  ingress {
    from_port        = 443
    to_port          = 443
    protocol         = "tcp"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "load-balancer-security-group"
  }
}

resource "aws_security_group" "application" {
  name        = "application-security-group"
  description = "Security group for EC2 instances hosting web applications"
  vpc_id      = aws_vpc.csye-6225-vpc.id

  ingress {
    from_port       = 22
    to_port         = 22
    protocol        = "tcp"
    security_groups = [aws_security_group.load_balancer.id]
  }

  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.load_balancer.id]
  }


  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "application-security-group"
  }
}

# Database Security Group
resource "aws_security_group" "database" {
  name        = "database-security-group"
  description = "Security group for RDS instances"
  vpc_id      = aws_vpc.csye-6225-vpc.id

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.application.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "database-security-group"
  }
}


# RDS Subnet Group
resource "aws_db_subnet_group" "private" {
  name       = "csye6225-db-subnet-group"
  subnet_ids = aws_subnet.private[*].id

  tags = {
    Name = "CSYE6225 DB subnet group"
  }
}

# RDS Parameter Group
resource "aws_db_parameter_group" "csye6225" {
  family = "postgres15"
  name   = "csye6225-pg"

  parameter {
    name  = "log_connections"
    value = "1"
  }
}

# Lambda Security Group
resource "aws_security_group" "lambda_sg" {
  name        = "lambda-security-group"
  description = "Security group for Lambda function"
  vpc_id      = aws_vpc.csye-6225-vpc.id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# Route 53 Record
resource "aws_route53_record" "webapp" {
  zone_id = var.hosted_zone_id
  name    = var.hosted_record_name
  type    = "A"

  alias {
    name                   = aws_lb.webapp.dns_name
    zone_id                = aws_lb.webapp.zone_id
    evaluate_target_health = true
  }
}

# Application Load Balancer
resource "aws_lb" "webapp" {
  name               = "webapp-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.load_balancer.id]
  subnets            = aws_subnet.public[*].id

  tags = {
    Name = "webapp-alb"
  }
}

# ALB Target Group
resource "aws_lb_target_group" "webapp" {
  name     = "webapp-target-group"
  port     = 8080
  protocol = "HTTP"
  vpc_id   = aws_vpc.csye-6225-vpc.id

  health_check {
    enabled             = true
    path                = "/healthz"
    port                = "8080"
    healthy_threshold   = 2
    unhealthy_threshold = 2
    timeout             = 3
    interval            = 30
  }
}

# ALB Listener
resource "aws_lb_listener" "webapp" {
  load_balancer_arn = aws_lb.webapp.arn
  port              = "443"
  protocol          = "HTTPS"
  
  ssl_policy      = "ELBSecurityPolicy-2016-08"
  certificate_arn = aws_acm_certificate.webapp.arn


  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.webapp.arn
  }
}

# Create EC2 Key Pair
resource "aws_key_pair" "deployer" {
  key_name   = "webapp-key-pair"
  public_key = tls_private_key.webapp_key.public_key_openssh
}

# Generate private key
resource "tls_private_key" "webapp_key" {
  algorithm = "RSA"
  rsa_bits  = 4096
}

# Launch Template
resource "aws_launch_template" "webapp" {
  name          = "csye6225_asg"
  image_id      = var.custom_ami
  instance_type = "t2.medium"

  network_interfaces {
    associate_public_ip_address = true
    security_groups             = [aws_security_group.application.id]
  }

  iam_instance_profile {
    name = aws_iam_instance_profile.ec2_instance_profile.name
  }

  key_name = aws_key_pair.deployer.key_name

  user_data = base64encode(<<-EOF
              #!/bin/bash
              LOG_FILE="/var/log/user-data.log"
              exec > >(tee -a $LOG_FILE) 2>&1

              echo "Starting user data script execution..."

              echo "Updating apt repositories..."
              apt update || echo "Failed to update apt repositories"

              echo "Installing required packages (jq and unzip)..."
              apt install -y jq unzip || echo "Failed to install required packages"


              echo "Installing AWS CLI..."
              curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip" || echo "Failed to download AWS CLI installer"
              unzip awscliv2.zip || echo "Failed to unzip AWS CLI installer"
              sudo ./aws/install || echo "Failed to install AWS CLI"

              # Pass the KMS Key ARN to the application configuration
              echo "Updating application configuration with KMS Key ARN..."
              sed -i 's|AWS_S3_KMS_ARN_PLACEHOLDER|${aws_kms_key.s3_key.arn}|g' /etc/csye6225/application-env || echo "Failed to update KMS Key ARN"

              echo "Retrieving database credentials from Secrets Manager..."
              DB_CREDENTIALS=$(aws secretsmanager get-secret-value --secret-id ${aws_secretsmanager_secret.db_password.id} --query SecretString --output text)
              if [ $? -ne 0 ]; then
                echo "Error: Failed to retrieve database credentials from Secrets Manager."
              fi

              echo "Retrieving database endpoint..."
              DB_ENDPOINT=$(aws rds describe-db-instances --db-instance-identifier csye6225 --query 'DBInstances[0].Endpoint.Address' --output text)
              if [ $? -ne 0 ]; then
                echo "Error: Failed to retrieve database endpoint."
              fi

              echo "Parsing database credentials..."
              DB_USERNAME=$(echo $DB_CREDENTIALS | jq -r .username) || echo "Failed to parse DB_USERNAME"
              DB_PASSWORD=$(echo $DB_CREDENTIALS | jq -r .password) || echo "Failed to parse DB_PASSWORD"

              echo "Updating configuration with retrieved credentials and endpoint..."
              sed -i "s|DB_ENDPOINT_PLACEHOLDER|$DB_ENDPOINT|g" /etc/csye6225/application-env || echo "Failed to update DB endpoint"
              sed -i "s|DB_USERNAME_PLACEHOLDER|$DB_USERNAME|g" /etc/csye6225/application-env || echo "Failed to update DB username"
              sed -i "s|DB_PASSWORD_PLACEHOLDER|$DB_PASSWORD|g" /etc/csye6225/application-env || echo "Failed to update DB password"
              
              sed -i 's|AWS_S3_BUCKET_NAME_PLACEHOLDER|${aws_s3_bucket.user_pictures.id}|g' /etc/csye6225/application-env || echo "Failed to update S3 bucket"
              sed -i 's|SNS_TOPIC_ARN_PLACEHOLDER|${aws_sns_topic.user_verification.arn}|g' /etc/csye6225/application-env || echo "Failed to update SNS topic ARN"

              echo "Configuring CloudWatch Agent..."
              sed -i 's|CLOUDWATCH_LOG_GROUP_PLACEHOLDER|/csye6225/application|g' /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json || echo "Failed to update CloudWatch log group"

              echo "Starting CloudWatch Agent..."
              sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -s -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json || echo "Failed to fetch CloudWatch config"
              sudo systemctl enable amazon-cloudwatch-agent || echo "Failed to enable CloudWatch Agent"
              sudo systemctl start amazon-cloudwatch-agent || echo "Failed to start CloudWatch Agent"

              echo "Restarting application service..."
              sudo systemctl restart csye6225.service || echo "Failed to restart application service"

              echo "User data script execution completed. Logs are stored in $LOG_FILE"
              EOF
  )

  depends_on = [
    aws_db_instance.csye6225,
    aws_s3_bucket.user_pictures,
    aws_sns_topic.user_verification
  ]

  tag_specifications {
    resource_type = "instance"
    tags = {
      Name = "WebApp-ASG-Instance"
    }
  }
}

# Output RDS Endpoint
output "rds_endpoint" {
  value = aws_db_instance.csye6225.endpoint
}

# Internet Gateway
resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.csye-6225-vpc.id

  tags = {
    Name = var.igw_name
  }
}

# IAM Role for EC2
resource "aws_iam_role" "ec2_role" {
  name = "ec2_s3_access_role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })
}

# IAM Policy to allow S3 access
resource "aws_iam_policy" "s3_access_policy" {
  name        = "S3AccessPolicy"
  description = "Policy to allow EC2 instance to access S3 bucket"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:ListBucket",
          "s3:GetObject",
          "s3:PutObject",
          "s3:DeleteObject"
        ]
        Resource = [
          aws_s3_bucket.user_pictures.arn,
          "${aws_s3_bucket.user_pictures.arn}/*"
        ]
      }
    ]
  })
}

# CloudWatch IAM Policy
resource "aws_iam_policy" "cloudwatch_policy" {
  name        = "CloudWatchPolicy"
  description = "Policy to allow CloudWatch agent to publish metrics and logs"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "cloudwatch:PutMetricData",
          "ec2:DescribeVolumes",
          "ec2:DescribeTags",
          "logs:PutLogEvents",
          "logs:DescribeLogStreams",
          "logs:DescribeLogGroups",
          "logs:CreateLogStream",
          "logs:CreateLogGroup"
        ]
        Resource = "*"
      }
    ]
  })
}

# Lambda IAM Role
resource "aws_iam_role" "lambda_role" {
  name = "email_verification_lambda_role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })
}

# Lambda IAM Role Policy for VPC Access
resource "aws_iam_role_policy" "lambda_vpc_policy" {
  name = "lambda_vpc_policy"
  role = aws_iam_role.lambda_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ec2:CreateNetworkInterface",
          "ec2:DescribeNetworkInterfaces",
          "ec2:DeleteNetworkInterface"
        ]
        Resource = "*"
      }
    ]
  })
}

# IAM Role Policy for Secrets Manager and KMS Access
resource "aws_iam_role_policy" "lambda_secrets_access_policy" {
  name = "lambda_secrets_access_policy"
  role = aws_iam_role.lambda_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue"
        ]
        Resource = aws_secretsmanager_secret.email_credentials.arn
      },
      {
        Effect = "Allow"
        Action = [
          "kms:Decrypt"
        ]
        Resource = aws_kms_key.secrets_key.arn
      }
    ]
  })
}



# Lambda IAM Role Policy for Logging to CloudWatch
resource "aws_iam_role_policy" "lambda_logging_policy" {
  name = "lambda_logging_policy"
  role = aws_iam_role.lambda_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Resource = "arn:aws:logs:*:*:*"
      }
    ]
  })
}

# EC2 IAM Role Policy for accessing secrets
resource "aws_iam_policy" "ec2_secrets_access_policy" {
  name        = "ec2_secrets_access_policy"
  description = "Allows EC2 to retrieve secrets and decrypt them using KMS."

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "SecretsManagerAccess"
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue"
        ]
        Resource = aws_secretsmanager_secret.db_password.arn
      },
      {
        Sid    = "KMSDecryptAccess"
        Effect = "Allow"
        Action = [
          "kms:Decrypt",
          "kms:GenerateDataKey*",
          "kms:DescribeKey"
        ]
        Resource = aws_kms_key.secrets_key.arn
      }
    ]
  })
}

# EC2 IAM Role Policy for describe rds
resource "aws_iam_role_policy" "ec2_rds_policy" {
  name = "ec2-rds-policy"
  role = aws_iam_role.ec2_role.name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "rds:DescribeDBInstances"
        ]
        Resource = "*"
      }
    ]
  })
}

# Attach CloudWatch policy to the EC2 role
resource "aws_iam_role_policy_attachment" "attach_cloudwatch_policy" {
  role       = aws_iam_role.ec2_role.name
  policy_arn = aws_iam_policy.cloudwatch_policy.arn
}

# Attach the S3 policy to the role
resource "aws_iam_role_policy_attachment" "attach_s3_access_policy" {
  role       = aws_iam_role.ec2_role.name
  policy_arn = aws_iam_policy.s3_access_policy.arn
}

# Create IAM Instance Profile for EC2
resource "aws_iam_instance_profile" "ec2_instance_profile" {
  name = "ec2_s3_instance_profile"
  role = aws_iam_role.ec2_role.name
}

# # Attach the Secret policy to the role
resource "aws_iam_role_policy_attachment" "ec2_secrets_policy_attachment" {
  role       = aws_iam_role.ec2_role.name
  policy_arn = aws_iam_policy.ec2_secrets_access_policy.arn
}

resource "aws_iam_policy" "s3_kms_full_access_policy" {
  name        = "s3_kms_full_access_policy"
  description = "Provides full access to S3 and KMS for the specified key"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:PutObject",
          "s3:GetObject",
          "s3:ListBucket"
        ]
        Resource = [
          "arn:aws:s3:::${aws_s3_bucket.user_pictures.bucket}",
          "arn:aws:s3:::${aws_s3_bucket.user_pictures.bucket}/*"
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "kms:Encrypt",
          "kms:Decrypt",
          "kms:ReEncrypt*",
          "kms:GenerateDataKey*",
          "kms:DescribeKey"
        ]
        Resource = aws_kms_key.s3_key.arn
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "attach_s3_kms_full_access_policy" {
  role       = aws_iam_role.ec2_role.name
  policy_arn = aws_iam_policy.s3_kms_full_access_policy.arn
}

# RDS Instance
resource "aws_db_instance" "csye6225" {
  identifier        = "csye6225"
  instance_class    = "db.t3.micro"
  allocated_storage = 20
  engine            = "postgres"
  engine_version    = "15"
  db_name           = "csye6225"
  username          = var.db_username
  password          = random_password.db_password.result


  db_subnet_group_name   = aws_db_subnet_group.private.name
  vpc_security_group_ids = [aws_security_group.database.id]

  parameter_group_name = aws_db_parameter_group.csye6225.name

  kms_key_id        = aws_kms_key.rds_key.arn
  storage_encrypted = true

  multi_az            = false
  publicly_accessible = false
  skip_final_snapshot = true

  tags = {
    Name = "CSYE6225 Database"
  }
}

# Generate a random UUID for bucket name
resource "random_uuid" "bucket_uuid" {}

# Create S3 bucket
resource "aws_s3_bucket" "user_pictures" {
  bucket        = "user-pictures-${random_uuid.bucket_uuid.result}"
  force_destroy = true # Allows Terraform to delete non-empty bucket
}

# Enable default encryption
resource "aws_s3_bucket_server_side_encryption_configuration" "bucket_encryption" {
  bucket = aws_s3_bucket.user_pictures.id

  rule {
    apply_server_side_encryption_by_default {
      kms_master_key_id = aws_kms_key.s3_key.arn
      sse_algorithm     = "aws:kms"
    }
  }
}

# Configure lifecycle rule
resource "aws_s3_bucket_lifecycle_configuration" "bucket_lifecycle" {
  bucket = aws_s3_bucket.user_pictures.id

  rule {
    id     = "transition_to_ia"
    status = "Enabled"

    transition {
      days          = 30
      storage_class = "STANDARD_IA"
    }
  }
}

# Make bucket private
resource "aws_s3_bucket_public_access_block" "bucket_public_access_block" {
  bucket = aws_s3_bucket.user_pictures.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Output the bucket name
output "bucket_name" {
  value = aws_s3_bucket.user_pictures.id
}

# Auto Scaling Group
resource "aws_autoscaling_group" "webapp" {
  name                      = "webapp-asg"
  desired_capacity          = 1
  max_size                  = 5
  min_size                  = 1
  target_group_arns         = [aws_lb_target_group.webapp.arn]
  vpc_zone_identifier       = aws_subnet.public[*].id
  health_check_type         = "EC2"
  health_check_grace_period = 300

  launch_template {
    id      = aws_launch_template.webapp.id
    version = "$Latest"
  }

  tag {
    key                 = "Name"
    value               = "WebApp-ASG-Instance"
    propagate_at_launch = true
  }
}

# Scale Up Policy
resource "aws_autoscaling_policy" "scale_up" {
  name                   = "webapp-scale-up"
  scaling_adjustment     = 1
  adjustment_type        = "ChangeInCapacity"
  cooldown               = 60
  autoscaling_group_name = aws_autoscaling_group.webapp.name
  policy_type            = "SimpleScaling"
}

resource "aws_cloudwatch_metric_alarm" "high_cpu" {
  alarm_name          = "webapp-high-cpu"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 2
  metric_name         = "CPUUtilization"
  namespace           = "AWS/EC2"
  period              = 60
  statistic           = "Average"
  threshold           = 30.0
  alarm_actions       = [aws_autoscaling_policy.scale_up.arn]
  dimensions = {
    AutoScalingGroupName = aws_autoscaling_group.webapp.name
  }
}

# Scale Down Policy
resource "aws_autoscaling_policy" "scale_down" {
  name                   = "webapp-scale-down"
  scaling_adjustment     = -1
  adjustment_type        = "ChangeInCapacity"
  cooldown               = 60
  autoscaling_group_name = aws_autoscaling_group.webapp.name
  policy_type            = "SimpleScaling"
}

resource "aws_cloudwatch_metric_alarm" "low_cpu" {
  alarm_name          = "webapp-low-cpu"
  comparison_operator = "LessThanOrEqualToThreshold"
  evaluation_periods  = 2
  metric_name         = "CPUUtilization"
  namespace           = "AWS/EC2"
  period              = 60
  statistic           = "Average"
  threshold           = 5.0
  alarm_actions       = [aws_autoscaling_policy.scale_down.arn]
  dimensions = {
    AutoScalingGroupName = aws_autoscaling_group.webapp.name
  }
}


# Lambda Function
resource "aws_lambda_function" "email_verification" {
  filename      = var.lambda_deployment_path
  function_name = "email-verification"
  role          = aws_iam_role.lambda_role.arn
  handler       = "com.example.lambda.EmailVerificationLambda::handleRequest"
  runtime       = "java17"
  timeout       = 300
  memory_size   = 512

  environment {
    variables = {
      EMAIL_SECRET_NAME = aws_secretsmanager_secret.email_credentials.name
    }
  }

  vpc_config {
    subnet_ids         = aws_subnet.private[*].id
    security_group_ids = [aws_security_group.lambda_sg.id]
  }
}

# SNS Topic
resource "aws_sns_topic" "user_verification" {
  name = "user-verification-topic"

  tags = {
    Name = "User Verification Topic"
  }
}

# SNS Topic Policy
resource "aws_sns_topic_policy" "default" {
  arn = aws_sns_topic.user_verification.arn

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "AllowEC2ToPublishToSNS"
        Effect = "Allow"
        Principal = {
          AWS = aws_iam_role.ec2_role.arn
        }
        Action   = "sns:Publish"
        Resource = aws_sns_topic.user_verification.arn
      }
    ]
  })
}

# Add SNS publish permission to EC2 instance role
resource "aws_iam_role_policy" "ec2_sns_policy" {
  name = "ec2_sns_policy"
  role = aws_iam_role.ec2_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "sns:Publish"
        ]
        Resource = [
          aws_sns_topic.user_verification.arn
        ]
      }
    ]
  })
}

# SNS Topic Subscription
resource "aws_sns_topic_subscription" "lambda" {
  topic_arn = aws_sns_topic.user_verification.arn
  protocol  = "lambda"
  endpoint  = aws_lambda_function.email_verification.arn
}

# Lambda Permission for SNS
resource "aws_lambda_permission" "with_sns" {
  statement_id  = "AllowExecutionFromSNS"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.email_verification.function_name
  principal     = "sns.amazonaws.com"
  source_arn    = aws_sns_topic.user_verification.arn
}

output "sns_topic_arn" {
  value = aws_sns_topic.user_verification.arn
}

resource "aws_nat_gateway" "nat" {
  allocation_id = aws_eip.nat.id
  subnet_id     = aws_subnet.public[0].id

  tags = {
    Name = "nat-gateway"
  }
}

resource "aws_eip" "nat" {
}



# Generate random suffix for unique resource naming
resource "random_string" "kms_suffix" {
  length  = 8
  special = false
  upper   = false
}

# Generate a secure random password for the database
resource "random_password" "db_password" {
  length           = 16
  special          = false
  #override_special = "!#$%&*()-_=+[]{}<>:?"
}

#  KMS Key for EC2 (previously shown configuration)
resource "aws_kms_key" "ec2_key" {
  description             = "KMS Key for EC2 Encryption"
  deletion_window_in_days = 10
  enable_key_rotation     = true
  rotation_period_in_days = 90

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "Enable IAM User Permissions"
        Effect = "Allow"
        Principal = {
          AWS = "arn:aws:iam::${var.aws_account_id}:root"
        }
        Action   = "kms:*"
        Resource = "*"
      },
      {
        Sid    = "Allow EC2 Service to Use Key"
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
        Action = [
          "kms:Encrypt",
          "kms:Decrypt",
          "kms:ReEncrypt*",
          "kms:GenerateDataKey*",
          "kms:DescribeKey"
        ]
        Resource = "*"
      }
    ]
  })

  tags = {
    Name = "EC2-KMS-Key-${random_string.kms_suffix.result}"
  }
}

#  KMS Key for RDS (updated with similar restrictive policy)
resource "aws_kms_key" "rds_key" {
  description             = "KMS Key for RDS Encryption"
  deletion_window_in_days = 10
  enable_key_rotation     = true
  rotation_period_in_days = 90

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "Enable IAM User Permissions"
        Effect = "Allow"
        Principal = {
          AWS = "arn:aws:iam::${var.aws_account_id}:root"
        }
        Action   = "kms:*"
        Resource = "*"
      },
      {
        Sid    = "Allow RDS Service to Use Key"
        Effect = "Allow"
        Principal = {
          Service = "rds.amazonaws.com"
        }
        Action = [
          "kms:Encrypt",
          "kms:Decrypt",
          "kms:ReEncrypt*",
          "kms:GenerateDataKey*",
          "kms:DescribeKey"
        ]
        Resource = "*"
      }
    ]
  })

  tags = {
    Name = "RDS-KMS-Key-${random_string.kms_suffix.result}"
  }
}

#  KMS Key for S3 (similar configuration to previous version)
resource "aws_kms_key" "s3_key" {
  description             = "KMS Key for S3 Bucket Encryption"
  deletion_window_in_days = 10
  enable_key_rotation     = true
  rotation_period_in_days = 90

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "Enable IAM User Permissions"
        Effect = "Allow"
        Principal = {
          AWS = "arn:aws:iam::${var.aws_account_id}:root"
        }
        Action   = "kms:*"
        Resource = "*"
      },
      {
        Sid    = "Allow S3 Service to Use Key"
        Effect = "Allow"
        Principal = {
          Service = "s3.amazonaws.com"
        }
        Action = [
          "kms:Encrypt",
          "kms:Decrypt",
          "kms:ReEncrypt*",
          "kms:GenerateDataKey*",
          "kms:DescribeKey"
        ]
        Resource = "*"
      }
    ]
  })

  tags = {
    Name = "S3-KMS-Key-${random_string.kms_suffix.result}"
  }
}

#  KMS Key for Secrets Manager
resource "aws_kms_key" "secrets_key" {
  description             = "KMS Key for Secrets Manager Encryption"
  deletion_window_in_days = 10
  enable_key_rotation     = true
  rotation_period_in_days = 90

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "Enable IAM User Permissions"
        Effect = "Allow"
        Principal = {
          AWS = "arn:aws:iam::${var.aws_account_id}:root"
        }
        Action   = "kms:*"
        Resource = "*"
      },
      {
        Sid    = "Allow EC2 Service to Use Key"
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
        Action = [
          "kms:Encrypt",
          "kms:Decrypt",
          "kms:ReEncrypt*",
          "kms:GenerateDataKey*",
          "kms:DescribeKey"
        ]
        Resource = "*"
      },
      {
        Sid    = "Allow Secrets Manager to Use Key"
        Effect = "Allow"
        Principal = {
          Service = "secretsmanager.amazonaws.com"
        }
        Action = [
          "kms:Decrypt",
          "kms:GenerateDataKey*"
        ]
        Resource = "*"
      }
    ]
  })

  tags = {
    Name = "Secrets-KMS-Key-${random_string.kms_suffix.result}"
  }
}

# Secret for Database Password
resource "aws_secretsmanager_secret" "db_password" {
  name        = "db-password-${random_string.kms_suffix.result}"
  description = "RDS Database Password"
  kms_key_id  = aws_kms_key.secrets_key.key_id
}

# # Store the generated database password in Secrets Manager
resource "aws_secretsmanager_secret_version" "db_password" {
  secret_id = aws_secretsmanager_secret.db_password.id
  secret_string = jsonencode({
    username = var.db_username
    password = random_password.db_password.result
  })
}

#  Secret for Email Service Credentials
resource "aws_secretsmanager_secret" "email_credentials" {
  name        = "email-credentials-${random_string.kms_suffix.result}"
  description = "Email Service Credentials for Lambda"
  kms_key_id  = aws_kms_key.secrets_key.key_id
}

# Store the SendGrid credentials in Secrets Manager
resource "aws_secretsmanager_secret_version" "email_credentials" {
  secret_id = aws_secretsmanager_secret.email_credentials.id
  secret_string = jsonencode({
    sendgrid_api_key      = var.sendgrid_api_key
    sendgrid_sender_email = var.sendgrid_sender_email
    verify_email_endpoint = var.verify_email_endpoint
  })
}

resource "aws_acm_certificate" "webapp" {
  private_key       = file(var.cert_private_key)
  certificate_body  = file(var.cert_body)
  certificate_chain = file(var.cert_chain)

  lifecycle {
    create_before_destroy = true
  }

  tags = {
    Name = "webapp-demo-certificate"
  }
}













