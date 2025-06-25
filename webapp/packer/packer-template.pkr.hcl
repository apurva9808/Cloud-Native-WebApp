packer {
  required_plugins {
    amazon = {
      version = ">= 1.0.0"
      source  = "github.com/hashicorp/amazon"
    }
  }
}

variable "aws_region" {
  type    = string
  default = "us-east-1"
}

variable "subnet_id" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "ami_users" {
  type = list(string)
}

variable "db_name" {
  type    = string
  default = "csye6225"
}

variable "db_endpoint" {
  type    = string
  default = "DB_ENDPOINT_PLACEHOLDER"
}

variable "db_username" {
  type    = string
  default = "DB_USERNAME_PLACEHOLDER"
}

variable "db_password" {
  type    = string
  default = "DB_PASSWORD_PLACEHOLDER"
}

variable "sns_topic_arn" {
  type = string
}

source "amazon-ebs" "ubuntu" {
  region        = var.aws_region
  ami_name      = "csye6225-${formatdate("YYYY-MM-DD-hh-mm-ss", timestamp())}"
  ami_users     = var.ami_users
  ami_groups    = []
  instance_type = "t2.medium"
  source_ami_filter {
    filters = {
      name                = "ubuntu/images/*ubuntu-noble-24.04-amd64-server-*"
      root-device-type    = "ebs"
      virtualization-type = "hvm"
    }
    most_recent = true
    owners      = ["099720109477"]
  }
  ssh_username = "ubuntu"
  subnet_id    = var.subnet_id
  vpc_id       = var.vpc_id
}

build {
  sources = ["source.amazon-ebs.ubuntu"]

  # Run the setup script with environment variables
  provisioner "shell" {
    script = "setup.sh"
    environment_vars = [
      "DB_ENDPOINT=${var.db_endpoint}",
      "DB_NAME=${var.db_name}",
      "DB_USERNAME=${var.db_username}",
      "DB_PASSWORD=${var.db_password}",
      "SNS_TOPIC_ARN=${var.sns_topic_arn}"
    ]
  }

  # Upload the Spring Boot JAR file
  provisioner "file" {
    source      = "./../target/restapi-0.0.1-SNAPSHOT.jar"
    destination = "/tmp/springboot_app.jar"
  }

  # Move the JAR file and set permissions
  provisioner "shell" {
    inline = [
      "sudo mv /tmp/springboot_app.jar /opt/csye6225/app/",
      "sudo chown csye6225:csye6225 /opt/csye6225/app/springboot_app.jar",
      "sudo chmod 500 /opt/csye6225/app/springboot_app.jar"
    ]
  }

  # Upload the systemd service file to the instance
  provisioner "file" {
    source      = "csye6225.service"
    destination = "/tmp/csye6225.service"
  }

  # Move the systemd service file and enable the service
  provisioner "shell" {
    inline = [
      "sudo mv /tmp/csye6225.service /etc/systemd/system/csye6225.service",
      "sudo systemctl daemon-reload",
      "sudo systemctl enable csye6225.service"
    ]
  }
}
