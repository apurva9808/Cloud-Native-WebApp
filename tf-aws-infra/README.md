# tf-aws-infra
# Terraform AWS Infrastructure

This project uses Terraform to manage and provision AWS infrastructure, including VPC, subnets, Internet Gateway, route tables, and other networking components.

## Prerequisites

Before you begin, ensure you have the following installed:

- [Terraform](https://www.terraform.io/downloads.html) (version 1.0+ recommended)
- AWS CLI configured with appropriate credentials
- Git (optional, for version control)

## Project Structure

The main Terraform files in this project are:

- `main.tf`: Main configuration file to orchestrate the infrastructure.
 Configures the AWS provider.
Defines variables used in the configuration.
Configures the VPC.
 Configures public and private subnets.
Configures route tables and associations.
 Configures the Internet Gateway.
 Specifies the output values of the resources created.
 Variable files for different environments (development, demo).
 Specifies which files and directories to ignore in version control.

## Setup

1. **Clone the Repository**

   ```bash
   git clone <repository-url>
   cd <repository-directory>


applied load balancer

terraform formate

terraform valid and formate