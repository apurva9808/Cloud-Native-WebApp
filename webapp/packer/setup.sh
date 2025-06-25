#!/bin/bash

# Extract database name from DB_URL
#DB_NAME=$(echo $DB_URL | awk -F'/' '{print $NF}')

sudo apt-get update
sudo apt-get upgrade -y
sudo apt-get install -y openjdk-17-jdk
#sudo apt install wget gnupg2 -y # prereq for postgres
#wget -qO - https://www.postgresql.org/media/keys/ACCC4CF8.asc | sudo tee /etc/apt/trusted.gpg.d/postgresql.asc # add postgres repo
#echo "deb http://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" | sudo tee /etc/apt/sources.list.d/pgdg.list
#sudo apt update
#sudo apt install postgresql-15 -y
sudo useradd -m -s /usr/sbin/nologin csye6225
sudo groupadd -f csye6225
sudo usermod -aG csye6225 csye6225
sudo mkdir -p /opt/csye6225
sudo chown csye6225:csye6225 /opt/csye6225
sudo -u csye6225 mkdir -p /opt/csye6225/app
sudo mkdir -p /etc/csye6225
sudo chown csye6225:csye6225 /etc/csye6225

sudo mkdir -p /var/log/csye6225
sudo chown csye6225:csye6225 /var/log/csye6225



echo "DB_HOST=${DB_ENDPOINT}" | sudo tee /etc/csye6225/application-env
echo "DB_NAME=${DB_NAME}" | sudo tee -a /etc/csye6225/application-env
echo "DB_USERNAME=${DB_USERNAME}" | sudo tee -a /etc/csye6225/application-env
echo "DB_PASSWORD=${DB_PASSWORD}" | sudo tee -a /etc/csye6225/application-env
echo "AWS_S3_BUCKET_NAME=AWS_S3_BUCKET_NAME_PLACEHOLDER" | sudo tee -a /etc/csye6225/application-env
echo "SNS_TOPIC_ARN=SNS_TOPIC_ARN_PLACEHOLDER" | sudo tee -a /etc/csye6225/application-env
sudo chmod 600 /etc/csye6225/application-env
#sudo chown csye6225:csye6225 /etc/csye6225/application-env
#
## PostgreSQL setup
#sudo -u postgres psql << EOF
#CREATE USER ${DB_USERNAME} WITH PASSWORD '${DB_PASSWORD}';
#CREATE DATABASE "${DB_NAME}";
#GRANT ALL PRIVILEGES ON DATABASE "${DB_NAME}" TO ${DB_USERNAME};
#ALTER USER ${DB_USERNAME} WITH SUPERUSER;
#GRANT CREATE ON SCHEMA public TO ${DB_USERNAME};
#GRANT USAGE ON SCHEMA public TO ${DB_USERNAME};
#GRANT ALL PRIVILEGES ON SCHEMA public TO ${DB_USERNAME};
#\du
#EOF

# Display user information
#sudo -u postgres psql -c '\du'

# Install CloudWatch Agent
wget https://s3.amazonaws.com/amazoncloudwatch-agent/ubuntu/amd64/latest/amazon-cloudwatch-agent.deb
sudo dpkg -i -E ./amazon-cloudwatch-agent.deb
rm amazon-cloudwatch-agent.deb

# Create CloudWatch Agent configuration directory
sudo mkdir -p /opt/aws/amazon-cloudwatch-agent/etc
sudo chown cwagent:cwagent /opt/aws/amazon-cloudwatch-agent/etc

# Create template CloudWatch config
sudo tee /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json > /dev/null << 'EOF'
{
  "agent": {
    "metrics_collection_interval": 60,
    "run_as_user": "cwagent"
  },
  "logs": {
    "logs_collected": {
      "files": {
        "collect_list": [
          {
            "file_path": "/var/log/csye6225/application.log",
            "log_group_name": "CLOUDWATCH_LOG_GROUP_PLACEHOLDER",
            "log_stream_name": "{instance_id}",
            "timezone": "UTC"
          }
        ]
      }
    }
  },
  "metrics": {
    "metrics_collected": {
      "statsd": {
        "service_address": ":8125",
        "metrics_collection_interval": 10,
        "metrics_aggregation_interval": 60
      }
    }
  }
}
EOF