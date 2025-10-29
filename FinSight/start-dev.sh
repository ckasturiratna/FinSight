#!/bin/bash

# FinSight Development Startup Script
# This script starts both the backend (Spring Boot) and frontend (React/Vite) simultaneously

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to check if a port is in use
port_in_use() {
    lsof -i :$1 >/dev/null 2>&1
}

# Function to cleanup background processes
cleanup() {
    print_status "Shutting down services..."
    if [ ! -z "$BACKEND_PID" ]; then
        kill $BACKEND_PID 2>/dev/null || true
    fi
    if [ ! -z "$FRONTEND_PID" ]; then
        kill $FRONTEND_PID 2>/dev/null || true
    fi
    print_success "Services stopped"
    exit 0
}

# Set up signal handlers
trap cleanup SIGINT SIGTERM

# Get the script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/finsight_backend"
FRONTEND_DIR="$SCRIPT_DIR/finsight_frontend"

print_status "Starting FinSight Development Environment"
print_status "Backend directory: $BACKEND_DIR"
print_status "Frontend directory: $FRONTEND_DIR"

# Check if directories exist
if [ ! -d "$BACKEND_DIR" ]; then
    print_error "Backend directory not found: $BACKEND_DIR"
    exit 1
fi

if [ ! -d "$FRONTEND_DIR" ]; then
    print_error "Frontend directory not found: $FRONTEND_DIR"
    exit 1
fi

# Check for required tools
print_status "Checking prerequisites..."

if ! command_exists java; then
    print_error "Java is not installed or not in PATH"
    exit 1
fi

if ! command_exists mvn; then
    print_error "Maven is not installed or not in PATH"
    exit 1
fi

if ! command_exists node; then
    print_error "Node.js is not installed or not in PATH"
    exit 1
fi

if ! command_exists npm; then
    print_error "npm is not installed or not in PATH"
    exit 1
fi

print_success "All prerequisites found"

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    print_warning "Java version $JAVA_VERSION detected. Spring Boot requires Java 21+"
fi

# Check if ports are available
if port_in_use 8080; then
    print_warning "Port 8080 is already in use. Backend may not start properly."
fi

if port_in_use 5173; then
    print_warning "Port 5173 is already in use. Frontend may not start properly."
fi

# Install frontend dependencies if node_modules doesn't exist
if [ ! -d "$FRONTEND_DIR/node_modules" ]; then
    print_status "Installing frontend dependencies..."
    cd "$FRONTEND_DIR"
    npm install
    cd "$SCRIPT_DIR"
    print_success "Frontend dependencies installed"
fi

# Start backend
print_status "Starting backend (Spring Boot)..."
cd "$BACKEND_DIR"
mvn spring-boot:run > ../backend.log 2>&1 &
BACKEND_PID=$!
cd "$SCRIPT_DIR"

# Wait a moment for backend to start
sleep 3

# Check if backend started successfully
if ! kill -0 $BACKEND_PID 2>/dev/null; then
    print_error "Backend failed to start. Check backend.log for details."
    exit 1
fi

print_success "Backend started (PID: $BACKEND_PID)"

# Start frontend
print_status "Starting frontend (React/Vite)..."
cd "$FRONTEND_DIR"
npm run dev > ../frontend.log 2>&1 &
FRONTEND_PID=$!
cd "$SCRIPT_DIR"

# Wait a moment for frontend to start
sleep 3

# Check if frontend started successfully
if ! kill -0 $FRONTEND_PID 2>/dev/null; then
    print_error "Frontend failed to start. Check frontend.log for details."
    cleanup
    exit 1
fi

print_success "Frontend started (PID: $FRONTEND_PID)"

print_success "FinSight Development Environment is running!"
echo ""
echo -e "${GREEN}Backend:${NC}  http://localhost:8080"
echo -e "${GREEN}Frontend:${NC} http://localhost:5173"
echo ""
echo -e "${YELLOW}Logs:${NC}"
echo -e "  Backend:  tail -f backend.log"
echo -e "  Frontend: tail -f frontend.log"
echo ""
echo -e "${YELLOW}Press Ctrl+C to stop all services${NC}"

# Wait for user to stop the services
wait
