#!/bin/bash

echo "=== AutoCompChem Production Server ==="
echo ""

# Function to extract version from pom.xml
get_version_from_pom() {
    if [[ -f "pom.xml" ]]; then
        # Extract version using grep and sed
        local version=$(grep -m1 "<version>" pom.xml | sed 's/.*<version>\(.*\)<\/version>.*/\1/' | tr -d '[:space:]')
        if [[ -n "$version" ]]; then
            echo "$version"
        else
            echo "unknown"
        fi
    else
        echo "unknown"
    fi
}

# Function to find JAR file with fallback options
find_jar_file() {
    local version="$1"
    
    # Primary JAR file name
    local primary_jar="target/autocompchem-${version}.jar"
    
    # Alternative JAR file names to check
    local alternatives=(
        "target/autocompchem-${version}-jar-with-dependencies.jar"
        "target/autocompchem.jar"
        "autocompchem-${version}.jar"
        "autocompchem.jar"
    )
    
    # Check primary JAR first
    if [[ -f "$primary_jar" ]]; then
        echo "$primary_jar"
        return 0
    fi
    
    # Check alternatives
    for jar_file in "${alternatives[@]}"; do
        if [[ -f "$jar_file" ]]; then
            echo "$jar_file"
            return 0
        fi
    done
    
    # If no JAR found, return the primary expected name
    echo "$primary_jar"
    return 1
}

# Configuration
SERVER_PORT="${SERVER_PORT:-8080}"
LOG_DIR="logs"
PID_FILE="$LOG_DIR/autocompchem-server.pid"
LOG_FILE="$LOG_DIR/autocompchem-server.log"

# Extract version dynamically from pom.xml
PROJECT_VERSION=$(get_version_from_pom)
JAR_FILE=$(find_jar_file "$PROJECT_VERSION")

MAX_MEMORY="${MAX_MEMORY:-2048m}"
MIN_MEMORY="${MIN_MEMORY:-512m}"

# Create logs directory
mkdir -p "$LOG_DIR"

# Function to show usage
show_usage() {
    echo "Usage: $0 [COMMAND] [OPTIONS]"
    echo ""
    echo "Commands:"
    echo "  start       Start the server in the background (default)"
    echo "  stop        Stop the running server"
    echo "  restart     Restart the server"
    echo "  status      Check server status"
    echo "  logs        Show server logs (tail -f)"
    echo "  health      Check server health endpoint"
    echo "  version     Show project version information"
    echo ""
    echo "Options:"
    echo "  --port PORT        Server port (default: 8080)"
    echo "  --max-memory MEM   Maximum JVM memory (default: 2048m)"
    echo "  --min-memory MEM   Minimum JVM memory (default: 512m)"
    echo "  --jar              Use JAR file instead of Maven"
    echo "  --foreground       Run in foreground instead of background"
    echo ""
    echo "Environment Variables:"
    echo "  SERVER_PORT        Server port"
    echo "  MAX_MEMORY         Maximum JVM memory"
    echo "  MIN_MEMORY         Minimum JVM memory"
    echo ""
    echo "Examples:"
    echo "  $0 start                    # Start server on port 8080"
    echo "  $0 start --port 9090        # Start server on port 9090"
    echo "  $0 stop                     # Stop the server"
    echo "  $0 status                   # Check if server is running"
    echo "  $0 logs                     # View server logs"
    echo "  $0 version                  # Show version information"
}

# Function to show version information
show_version() {
    echo "AutoCompChem Version Information:"
    echo "  Project Version: $PROJECT_VERSION"
    echo "  JAR File: $JAR_FILE"
    echo "  JAR Exists: $(if [[ -f "$JAR_FILE" ]]; then echo "✅ Yes"; else echo "❌ No"; fi)"
    
    if [[ -f "pom.xml" ]]; then
        echo "  POM File: ✅ Found"
    else
        echo "  POM File: ❌ Not found"
    fi
}

# Function to check if server is running
is_server_running() {
    local pid_running=false
    local server_responding=false
    
    # Check PID file (for background processes)
    if [[ -f "$PID_FILE" ]]; then
        local pid=$(cat "$PID_FILE")
        if ps -p "$pid" > /dev/null 2>&1; then
            pid_running=true
        else
            # PID file exists but process is dead, clean it up
            rm -f "$PID_FILE"
        fi
    fi
    
    # Check if server is actually responding (for both background and foreground)
    if curl -f -s "http://localhost:$SERVER_PORT/api/v1/autocompchem/health" > /dev/null 2>&1; then
        server_responding=true
    fi
    
    # Return true if either PID is running OR server is responding
    if [[ "$pid_running" == "true" ]] || [[ "$server_responding" == "true" ]]; then
        return 0
    else
        return 1
    fi
}

# Function to get server PID
get_server_pid() {
    if [[ -f "$PID_FILE" ]]; then
        cat "$PID_FILE"
    else
        # Try to find the process by port
        local pid=$(lsof -ti:$SERVER_PORT 2>/dev/null)
        if [[ -n "$pid" ]]; then
            echo "$pid"
        else
            echo "unknown"
        fi
    fi
}

# Function to wait for server to start
wait_for_server() {
    local max_attempts=30
    local attempt=1
    
    echo "⏳ Waiting for server to start..."
    
    while [[ $attempt -le $max_attempts ]]; do
        if curl -f -s "http://localhost:$SERVER_PORT/api/v1/autocompchem/health" > /dev/null 2>&1; then
            echo "✅ Server is running and responding"
            return 0
        fi
        
        echo -n "."
        sleep 2
        ((attempt++))
    done
    
    echo ""
    echo "❌ Server failed to start or is not responding"
    return 1
}

# Function to start server
start_server() {
    local use_jar="$1"
    local foreground="$2"
    
    if is_server_running; then
        echo "⚠️  Server is already running (PID: $(get_server_pid))"
        echo "   Use '$0 stop' to stop it first, or '$0 restart' to restart"
        return 1
    fi
    
    echo "🚀 Starting AutoCompChem server..."
    echo "   Version: $PROJECT_VERSION"
    echo "   Port: $SERVER_PORT"
    echo "   Memory: $MIN_MEMORY - $MAX_MEMORY"
    echo "   Log file: $LOG_FILE"
    
    if [[ "$use_jar" == "true" ]]; then
        echo "   JAR file: $JAR_FILE"
        
        # Check if version was successfully extracted
        if [[ "$PROJECT_VERSION" == "unknown" ]]; then
            echo "⚠️  Warning: Could not extract version from pom.xml"
            echo "   Falling back to JAR file detection..."
        fi
        
        # Check if JAR file exists, build if it doesn't
        if [[ ! -f "$JAR_FILE" ]]; then
            echo "📦 JAR file not found, building..."
            echo "   Expected: $JAR_FILE"
            
            mvn clean package -DskipTests
            if [[ $? -ne 0 ]]; then
                echo "❌ Failed to build JAR file"
                return 1
            fi
            
            # Re-check for JAR file after build
            JAR_FILE=$(find_jar_file "$PROJECT_VERSION")
            if [[ ! -f "$JAR_FILE" ]]; then
                echo "❌ JAR file still not found after build: $JAR_FILE"
                echo "   Available JAR files in target/:"
                ls -la target/*.jar 2>/dev/null || echo "   No JAR files found"
                return 1
            fi
            echo "✅ Found JAR file: $JAR_FILE"
        else
            echo "✅ Using existing JAR file: $JAR_FILE"
        fi
        
        # Start with JAR
        local cmd="java -Xms$MIN_MEMORY -Xmx$MAX_MEMORY"
        cmd="$cmd -Dspring.profiles.active=production"
        cmd="$cmd -Dserver.port=$SERVER_PORT"
        cmd="$cmd -Dlogging.file.name=$LOG_FILE"
        cmd="$cmd -jar $JAR_FILE --server"
        
        if [[ "$foreground" == "true" ]]; then
            echo "🖥️  Running in foreground mode..."
            exec $cmd
        else
            echo "🔧 Starting in background..."
            nohup $cmd > "$LOG_FILE" 2>&1 &
            echo $! > "$PID_FILE"
        fi
    else
        # Start with Maven
        echo "   Mode: Maven (development)"
        
        local mvn_args="--server"
        mvn_args="$mvn_args --spring.profiles.active=production"
        mvn_args="$mvn_args --server.port=$SERVER_PORT"
        mvn_args="$mvn_args --logging.file.name=$LOG_FILE"
        
        local jvm_args="-Xms$MIN_MEMORY -Xmx$MAX_MEMORY"
        
        if [[ "$foreground" == "true" ]]; then
            echo "🖥️  Running in foreground mode..."
            exec mvn spring-boot:run \
                -Dspring-boot.run.arguments="$mvn_args" \
                -Dspring-boot.run.jvmArguments="$jvm_args"
        else
            echo "🔧 Starting in background..."
            nohup mvn spring-boot:run \
                -Dspring-boot.run.arguments="$mvn_args" \
                -Dspring-boot.run.jvmArguments="$jvm_args" \
                > "$LOG_FILE" 2>&1 &
            echo $! > "$PID_FILE"
        fi
    fi
    
    if [[ "$foreground" != "true" ]]; then
        # Wait for server to start
        if wait_for_server; then
            echo ""
            echo "🎉 AutoCompChem server started successfully!"
            echo "   PID: $(get_server_pid)"
            echo "   Version: $PROJECT_VERSION"
            echo "   URL: http://localhost:$SERVER_PORT"
            echo "   API Docs: http://localhost:$SERVER_PORT/swagger-ui.html"
            echo "   Health: http://localhost:$SERVER_PORT/api/v1/autocompchem/health"
            echo ""
            echo "📋 Useful commands:"
            echo "   $0 status    # Check server status"
            echo "   $0 logs      # View logs"
            echo "   $0 stop      # Stop server"
        else
            echo "❌ Server startup failed. Check logs: $LOG_FILE"
            return 1
        fi
    fi
}

# Function to stop server
stop_server() {
    if ! is_server_running; then
        echo "⚠️  Server is not running"
        return 1
    fi
    
    local pid=$(get_server_pid)
    local has_pid_file=false
    
    if [[ -f "$PID_FILE" ]]; then
        has_pid_file=true
        echo "🛑 Stopping AutoCompChem server (PID: $pid)..."
    else
        echo "🛑 Stopping AutoCompChem server (foreground process, PID: $pid)..."
        if [[ "$pid" == "unknown" ]]; then
            echo "⚠️  Could not determine PID. Trying to stop by port..."
            pid=$(lsof -ti:$SERVER_PORT 2>/dev/null)
            if [[ -z "$pid" ]]; then
                echo "❌ Could not find process using port $SERVER_PORT"
                return 1
            fi
        fi
    fi
    
    # Try graceful shutdown first
    if [[ "$pid" != "unknown" ]]; then
        kill "$pid" 2>/dev/null
        
        # Wait for graceful shutdown
        local attempts=0
        while [[ $attempts -lt 10 ]] && ps -p "$pid" > /dev/null 2>&1; do
            sleep 1
            ((attempts++))
        done
        
        # Force kill if still running
        if ps -p "$pid" > /dev/null 2>&1; then
            echo "⚡ Force killing server..."
            kill -9 "$pid" 2>/dev/null
        fi
    fi
    
    # Clean up PID file if it exists
    if [[ "$has_pid_file" == "true" ]]; then
        rm -f "$PID_FILE"
    fi
    
    # Verify server is stopped
    if is_server_running; then
        echo "⚠️  Server may still be running. Check manually."
        return 1
    else
        echo "✅ Server stopped"
    fi
}

# Function to show server status
show_status() {
    if is_server_running; then
        local pid=$(get_server_pid)
        local has_pid_file=false
        local mode="foreground"
        
        if [[ -f "$PID_FILE" ]]; then
            has_pid_file=true
            mode="background"
        fi
        
        echo "✅ AutoCompChem server is running"
        echo "   PID: $pid"
        echo "   Mode: $mode"
        echo "   Port: $SERVER_PORT"
        echo "   URL: http://localhost:$SERVER_PORT"
        
        # Check if server is responding
        if curl -f -s "http://localhost:$SERVER_PORT/api/v1/autocompchem/health" > /dev/null 2>&1; then
            echo "   Health: Responding to health checks ✅"
        else
            echo "   Health: Not responding to health checks ⚠️"
        fi
        
        # Show additional info based on mode
        if [[ "$has_pid_file" == "true" ]]; then
            echo "   Control: Use './server-manager.sh stop' to stop"
        else
            echo "   Control: Running in foreground (use Ctrl+C to stop)"
        fi
    else
        echo "❌ AutoCompChem server is not running"
    fi
}

# Function to show logs
show_logs() {
    if [[ -f "$LOG_FILE" ]]; then
        echo "📋 Server logs ($LOG_FILE):"
        echo "   Press Ctrl+C to exit"
        echo ""
        tail -f "$LOG_FILE"
    else
        echo "❌ Log file not found: $LOG_FILE"
    fi
}

# Function to check health endpoint
check_health() {
    echo "🔍 Checking server health..."
    
    if curl -f -s "http://localhost:$SERVER_PORT/api/v1/autocompchem/health" > /dev/null 2>&1; then
        local health_response=$(curl -s "http://localhost:$SERVER_PORT/api/v1/autocompchem/health")
        echo "✅ Server is healthy"
        echo "   Response: $health_response"
    else
        echo "❌ Server health check failed"
        echo "   URL: http://localhost:$SERVER_PORT/api/v1/autocompchem/health"
    fi
}

# Function to restart server
restart_server() {
    echo "🔄 Restarting AutoCompChem server..."
    
    if is_server_running; then
        stop_server
        sleep 2
    fi
    
    start_server "$USE_JAR" "$FOREGROUND"
}

# Parse command line arguments
COMMAND="${1:-start}"
USE_JAR="false"
FOREGROUND="false"

# Parse options
shift
while [[ $# -gt 0 ]]; do
    case $1 in
        --port)
            SERVER_PORT="$2"
            shift 2
            ;;
        --max-memory)
            MAX_MEMORY="$2"
            shift 2
            ;;
        --min-memory)
            MIN_MEMORY="$2"
            shift 2
            ;;
        --jar)
            USE_JAR="true"
            shift
            ;;
        --foreground)
            FOREGROUND="true"
            shift
            ;;
        --help|-h)
            show_usage
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
done

# Execute command
case "$COMMAND" in
    start)
        start_server "$USE_JAR" "$FOREGROUND"
        ;;
    stop)
        stop_server
        ;;
    restart)
        restart_server
        ;;
    status)
        show_status
        ;;
    logs)
        show_logs
        ;;
    health)
        check_health
        ;;
    version)
        show_version
        ;;
    help|--help|-h)
        show_usage
        ;;
    *)
        echo "Unknown command: $COMMAND"
        show_usage
        exit 1
        ;;
esac 