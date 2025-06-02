#!/bin/bash

echo "=== AutoCompChem Debug Server Launcher ==="
echo ""

# Create logs directory if it doesn't exist
mkdir -p logs

# Function to show available options
show_help() {
    echo "Usage: $0 [mode]"
    echo ""
    echo "Available modes:"
    echo "  1 | basic     - Basic debug mode with enhanced logging"
    echo "  2 | remote    - Remote debugging (port 5005) + enhanced logging"
    echo "  3 | trace     - Full trace mode with maximum logging"
    echo "  4 | profile   - Performance profiling mode"
    echo "  5 | jar       - Debug using JAR file instead of Maven"
    echo ""
    echo "Examples:"
    echo "  ./debug-server.sh 1           # Basic debug"
    echo "  ./debug-server.sh remote      # Remote debugging"
    echo "  ./debug-server.sh             # Interactive mode"
}

# Function for basic debug mode
basic_debug() {
    echo "🐛 Starting AutoCompChem in BASIC DEBUG mode..."
    echo "📝 Logs will be written to: logs/autocompchem-debug.log"
    echo "🌐 Server will be available at: http://localhost:8080"
    echo "📊 Actuator endpoints: http://localhost:8080/actuator"
    echo ""
    
    mvn spring-boot:run \
        -Dspring-boot.run.arguments="--server --spring.profiles.active=debug" \
        -Dspring-boot.run.jvmArguments="-Xms512m -Xmx1024m"
}

# Function for remote debugging
remote_debug() {
    echo "🐛 Starting AutoCompChem in REMOTE DEBUG mode..."
    echo "📝 Logs will be written to: logs/autocompchem-debug.log"
    echo "🌐 Server will be available at: http://localhost:8080"
    echo "🔌 Remote debugging port: 5005"
    echo "📊 Actuator endpoints: http://localhost:8080/actuator"
    echo ""
    echo "🔧 To connect with IDE:"
    echo "   - IntelliJ IDEA: Run -> Edit Configurations -> + -> Remote JVM Debug"
    echo "   - Eclipse: Debug Configurations -> Remote Java Application"
    echo "   - VS Code: Use Java debugger with remote attach configuration"
    echo ""
    
    mvn spring-boot:run \
        -Dspring-boot.run.arguments="--server --spring.profiles.active=debug" \
        -Dspring-boot.run.jvmArguments="-Xms512m -Xmx1024m -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
}

# Function for trace mode
trace_debug() {
    echo "🐛 Starting AutoCompChem in TRACE DEBUG mode..."
    echo "⚠️  WARNING: This will generate LOTS of logs!"
    echo "📝 Logs will be written to: logs/autocompchem-debug.log"
    echo "🌐 Server will be available at: http://localhost:8080"
    echo ""
    
    mvn spring-boot:run \
        -Dspring-boot.run.arguments="--server --spring.profiles.active=debug --trace=true --logging.level.org.springframework=DEBUG --logging.level.org.apache=DEBUG" \
        -Dspring-boot.run.jvmArguments="-Xms512m -Xmx1024m"
}

# Function for profiling mode
profile_debug() {
    echo "🐛 Starting AutoCompChem in PROFILING mode..."
    echo "📈 JVM profiling enabled"
    echo "📝 Logs will be written to: logs/autocompchem-debug.log"
    echo "🌐 Server will be available at: http://localhost:8080"
    echo ""
    
    mvn spring-boot:run \
        -Dspring-boot.run.arguments="--server --spring.profiles.active=debug" \
        -Dspring-boot.run.jvmArguments="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -Xloggc:logs/gc.log"
}

# Function for JAR debugging
jar_debug() {
    echo "🐛 Starting AutoCompChem JAR in DEBUG mode..."
    
    # Build if JAR doesn't exist
    if [ ! -f "target/autocompchem-3.0.0.jar" ]; then
        echo "📦 Building JAR file first..."
        mvn clean package -DskipTests
    fi
    
    echo "📝 Logs will be written to: logs/autocompchem-debug.log"
    echo "🌐 Server will be available at: http://localhost:8080"
    echo "🔌 Remote debugging port: 5005"
    echo ""
    
    java -Xms512m -Xmx1024m \
         -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005 \
         -Dspring.profiles.active=debug \
         -jar target/autocompchem-3.0.0.jar --server
}

# Interactive mode
interactive_mode() {
    echo "Select debug mode:"
    echo "1) Basic debug mode"
    echo "2) Remote debugging"
    echo "3) Trace mode (verbose)"
    echo "4) Performance profiling"
    echo "5) JAR file debugging"
    echo "6) Show help"
    echo ""
    read -p "Enter your choice (1-6): " choice
    
    case $choice in
        1) basic_debug ;;
        2) remote_debug ;;
        3) trace_debug ;;
        4) profile_debug ;;
        5) jar_debug ;;
        6) show_help ;;
        *) echo "Invalid choice. Use ./debug-server.sh for help." ;;
    esac
}

# Main script logic
case "${1:-interactive}" in
    "1"|"basic")     basic_debug ;;
    "2"|"remote")    remote_debug ;;
    "3"|"trace")     trace_debug ;;
    "4"|"profile")   profile_debug ;;
    "5"|"jar")       jar_debug ;;
    "help"|"-h"|"--help") show_help ;;
    "interactive")   interactive_mode ;;
    *) echo "Unknown option: $1"; show_help ;;
esac 