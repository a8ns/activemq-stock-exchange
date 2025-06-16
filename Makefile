# ActiveMQ Podman Makefile

IMAGE_NAME = activemq
CONTAINER_NAME = activemq
CONTEXT_DIR = ActiveMQ/

.PHONY: build run restart stop start clean clean-all help

# Build the container image
build:
	podman build -t $(IMAGE_NAME) $(CONTEXT_DIR)

# Run container (will fail if container with same name exists)
run:
	podman run -d --name $(CONTAINER_NAME) -p 61616:61616 -p 8161:8161 $(IMAGE_NAME)

# Force run - removes existing container and runs new one
force-run: clean run

# Restart existing container
restart:
	podman restart $(CONTAINER_NAME)

# Stop running container
stop:
	podman stop $(CONTAINER_NAME)

# Start stopped container
start:
	podman start $(CONTAINER_NAME)

# Remove container only
clean:
	-podman rm $(CONTAINER_NAME)

# Remove container and image
clean-all: clean
	-podman rmi $(IMAGE_NAME):latest

# Rebuild everything from scratch
rebuild: clean-all build

help:
	@echo "Available targets:"
	@echo "  build      - Build the ActiveMQ container image"
	@echo "  run        - Run new container (fails if name exists)"
	@echo "  force-run  - Remove existing container and run new one"
	@echo "  restart    - Restart existing container"
	@echo "  stop       - Stop running container"
	@echo "  start      - Start stopped container"
	@echo "  clean      - Remove container"
	@echo "  clean-all  - Remove container and image"
	@echo "  rebuild    - Clean everything and rebuild"
	@echo "  help       - Show this help message"