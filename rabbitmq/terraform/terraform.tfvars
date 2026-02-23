aws_region     = "us-west-2"
service_name   = "rabbitmq-broker"
cluster_name   = "cs6650-assignment3-cluster"
create_cluster = true   # first deployment: true. After cluster exists: false

rmq_user    = "guest"
rmq_pass    = "guest"
task_cpu    = "512"
task_memory = "1024"