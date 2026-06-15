# ==========================================
# Configuration Variables
# ==========================================
IMAGE_NAME     = jee-chat-app
CONTAINER_NAME = chat-server
PORT           = 8080

# ==========================================
# Rules
# ==========================================
.PHONY: all build run stop clean fclean re logs

# Default rule
all: build run

# Build the Docker image
build:
	@echo "=> Building Docker image: $(IMAGE_NAME)..."
	docker build -t $(IMAGE_NAME) .

# Run the container
run:
	@echo "=> Starting container: $(CONTAINER_NAME) on port $(PORT)..."
	docker run -d -p $(PORT):$(PORT) --name $(CONTAINER_NAME) --restart unless-stopped $(IMAGE_NAME)

# Stop and remove the running container
stop:
	@echo "=> Stopping and removing container: $(CONTAINER_NAME)..."
	docker rm -f $(CONTAINER_NAME) || true

# Clean up the container (same as stop)
clean: stop

# Full clean: remove the container AND the image
fclean: clean
	@echo "=> Removing Docker image: $(IMAGE_NAME)..."
	docker rmi $(IMAGE_NAME) || true

# The classic 're' rule: Stop, Rebuild, and Run
re: stop build run

# Tail the application logs
logs:
	docker logs -f $(CONTAINER_NAME)
