#!/bin/bash

set -e  # exit immediately on error

APP_NAME="ppslink-stanchart-service"
IMAGE_NAME="duncodi/${APP_NAME}"
VERSION="1.0.0"

echo "======================================"
echo " Building JAR for $APP_NAME"
echo "======================================"
mvn clean package -DskipTests

echo "======================================"
echo " Building Docker image: $IMAGE_NAME:$VERSION"
echo "======================================"
docker build -t ${IMAGE_NAME}:${VERSION} .

echo "======================================"
echo " Pushing Docker image: $IMAGE_NAME:$VERSION"
echo "======================================"
docker push ${IMAGE_NAME}:${VERSION}

echo "======================================"
echo " Tagging and pushing latest"
echo "======================================"
docker tag ${IMAGE_NAME}:${VERSION} ${IMAGE_NAME}:latest
docker push ${IMAGE_NAME}:latest

echo "======================================"
echo " ✅ Done! Image pushed as:"
echo "   - ${IMAGE_NAME}:${VERSION}"
echo "   - ${IMAGE_NAME}:latest"
echo "======================================"
