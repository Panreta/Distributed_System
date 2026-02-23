aws_region          = "us-west-2"
service_name        = "scs-service"
cluster_name        = "cs6650-assignment3-cluster"
ecr_repository_name = "scs-service"

# FILL IN AFTER DEPLOYING RABBITMQ AND CCA
rabbitmq_host = "REPLACE_WITH_RABBITMQ_PRIVATE_IP"
cca_base_url  = "http://REPLACE_WITH_CCA_PRIVATE_IP:8080"

rabbitmq_port = 5672
rabbitmq_user = "guest"
rabbitmq_pass = "guest"
rmq_queue     = "orders_queue"

task_cpu    = "512"
task_memory = "1024"