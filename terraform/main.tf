# Вариант IaC рядом с Compose: тот же стенд, но через Terraform + docker provider.
# В бою обычно уезжает в ECS/EKS + RDS и отдельный state/backend.

terraform {
  required_version = ">= 1.5.0"
  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "~> 3.0"
    }
  }
}

provider "docker" {}

resource "docker_network" "orbitto" {
  name = "orbitto-auth-net"
}

resource "docker_volume" "pgdata" {
  name = "orbitto-auth-pgdata"
}

resource "docker_image" "postgres" {
  name         = "postgres:16-alpine"
  keep_locally = true
}

resource "docker_container" "postgres" {
  name  = "orbitto-auth-postgres"
  image = docker_image.postgres.image_id

  networks_advanced {
    name = docker_network.orbitto.name
  }

  env = [
    "POSTGRES_DB=orbitto_auth",
    "POSTGRES_USER=orbitto",
    "POSTGRES_PASSWORD=orbitto",
  ]

  volumes {
    volume_name    = docker_volume.pgdata.name
    container_path = "/var/lib/postgresql/data"
  }

  ports {
    internal = 5432
    external = 5432
  }
}

# Сервис приложения: собери образ из корня (`docker build -t orbitto-auth:local .`),
# раскомментируй и поставь image = "orbitto-auth:local".

# resource "docker_container" "auth" {
#   name  = "orbitto-auth-service"
#   image = "orbitto-auth:local"
#   depends_on = [docker_container.postgres]
#   networks_advanced {
#     name = docker_network.orbitto.name
#   }
#   env = [
#     "DB_HOST=orbitto-auth-postgres",
#     "DB_PORT=5432",
#     "DB_NAME=orbitto_auth",
#     "DB_USER=orbitto",
#     "DB_PASSWORD=orbitto",
#     "JWT_SECRET=replace-with-strong-secret",
#   ]
#   ports {
#     internal = 9090
#     external = 9090
#   }
#   ports {
#     internal = 8080
#     external = 8080
#   }
# }

output "postgres_container" {
  value = docker_container.postgres.name
}
