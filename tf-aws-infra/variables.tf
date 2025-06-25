variable "aws_region" {
  description = "The AWS region to deploy resources"
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC"
  type        = string
}

variable "vpc_name" {
  description = "The name of the VPC"
  type        = string
}

variable "public_subnet_name" {
  description = "Public subnet name"
  type        = string
}

variable "private_subnet_name" {
  description = "Private subnet name"
  type        = string
}

variable "route_table_cidr" {
  description = "CIDR block for the route table"
  type        = string
}

variable "public_route_table_name" {
  description = "Public route table name"
  type        = string
}

variable "private_route_table_name" {
  description = "Private route table name"
  type        = string
}

variable "igw_name" {
  description = "Name for the internet gateway"
  type        = string
}

variable "custom_ami" {
  description = "Custom AMI ID"
  type        = string
}

variable "instance_name" {
  description = "Name of the EC2 instance"
  type        = string
}

variable "db_username" {
  description = "Database username"
  type        = string
}

variable "db_password" {
  description = "Database password"
  type        = string
}

variable "hosted_zone_id" {
  description = "Zone ID for the public hosted zone"
  type        = string
}

variable "hosted_record_name" {
  description = "Record name"
  type        = string
}

variable "sendgrid_api_key" {
  description = "Sendgrid API key"
  type        = string
}

variable "lambda_deployment_path" {
  description = "Absolute path for the lambda deployment package"
  type        = string
}

variable "sendgrid_sender_email" {
  description = "Sender email for email verification"
  type        = string
}

variable "verify_email_endpoint" {
  description = "Endpoint to verify user email"
  type        = string
}

variable "aws_account_id" {
  description = "AWS account ID"
  type        = string
}

variable "cert_private_key" {
  description = "File path for cert private key"
  type        = string
}

variable "cert_body" {
  description = "File path for certificate body"
  type        = string
}

variable "cert_chain" {
  description = "File path for certificate chain"
  type        = string
}

