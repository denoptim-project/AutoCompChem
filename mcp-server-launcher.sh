#!/bin/bash

echo "=== AutoCompChem MCP Server Launcher ==="
echo ""

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MCP_SERVER_DIR="$SCRIPT_DIR/src/main/python/mcp_server"

# Check if AutoCompChem server is running
check_autocompchem_server() {
    local base_url="${AUTOCOMPCHEM_BASE_URL:-http://localhost:8080}"
    echo "🔍 Checking AutoCompChem server at $base_url..."
    
    if curl -f -s "$base_url/api/v1/autocompchem/health" > /dev/null 2>&1; then
        echo "✅ AutoCompChem server is running and accessible"
        return 0
    else
        echo "❌ AutoCompChem server is not accessible at $base_url"
        echo ""
        echo "Please ensure AutoCompChem server is running:"
        echo ""
        echo "🚀 Production mode (recommended):"
        echo "   ./start-server.sh start"
        echo ""
        echo "🐛 Development/Debug mode:"
        echo "   ./debug-server.sh"
        echo ""
        echo "📦 Using Maven directly:"
        echo "   mvn spring-boot:run -Dspring-boot.run.arguments=\"--server\""
        echo ""
        echo "🌐 Verify server accessibility:"
        echo "   curl http://localhost:8080/api/v1/autocompchem/health"
        echo ""
        echo "💡 To use a different server URL:"
        echo "   export AUTOCOMPCHEM_BASE_URL=http://your-server:port"
        return 1
    fi
}

# Setup Python environment
setup_python_env() {
    echo "🐍 Setting up Python environment..."
    
    # Check if Python 3.10+ is available
    if ! python3 --version | grep -E "Python 3\.(1[0-9]|[2-9][0-9])" > /dev/null 2>&1; then
        echo "❌ Python 3.10+ is required"
        echo "Current version: $(python3 --version 2>/dev/null || echo 'Python not found')"
        return 1
    fi
    
    echo "✅ Python version: $(python3 --version)"
    
    # Check if virtual environment should be used
    if [[ "$USE_VIRTUAL_ENV" == "true" ]] || [[ -z "$VIRTUAL_ENV" && "$CREATE_VIRTUAL_ENV" == "true" ]]; then
        echo "🔧 Creating/activating virtual environment..."
        
        cd "$MCP_SERVER_DIR"
        
        if [[ ! -d "venv" ]]; then
            python3 -m venv venv
        fi
        
        source venv/bin/activate
        echo "✅ Virtual environment activated"
    fi
    
    # Install dependencies
    echo "📦 Installing/checking dependencies..."
    cd "$MCP_SERVER_DIR"
    
    if [[ -f "requirements.txt" ]]; then
        pip install -r requirements.txt
        echo "✅ Dependencies installed"
    else
        echo "❌ requirements.txt not found at $MCP_SERVER_DIR/requirements.txt"
        return 1
    fi
}

# Run the MCP server
run_mcp_server() {
    echo "🚀 Starting AutoCompChem MCP Server..."
    echo ""
    echo "Server Configuration:"
    echo "- AutoCompChem URL: ${AUTOCOMPCHEM_BASE_URL:-http://localhost:8080}"
    echo "- Python: $(python3 --version)"
    echo "- Working Directory: $MCP_SERVER_DIR"
    echo ""
    echo "The server will communicate via STDIO (for Claude Desktop integration)"
    echo "Press Ctrl+C to stop the server"
    echo ""
    
    cd "$MCP_SERVER_DIR"
    exec python3 autocompchem_mcp_server.py
}

# Show usage information
show_usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  --check-only          Only check if AutoCompChem server is accessible"
    echo "  --setup-only          Only setup Python environment, don't run server"
    echo "  --no-venv            Don't use virtual environment"
    echo "  --create-venv        Create virtual environment if it doesn't exist"
    echo "  --help               Show this help message"
    echo ""
    echo "Environment Variables:"
    echo "  AUTOCOMPCHEM_BASE_URL    AutoCompChem server URL (default: http://localhost:8080)"
    echo "  USE_VIRTUAL_ENV          Force use of virtual environment (true/false)"
    echo "  CREATE_VIRTUAL_ENV       Create virtual environment if needed (true/false)"
    echo ""
    echo "Examples:"
    echo "  $0                                    # Run with default settings"
    echo "  $0 --create-venv                     # Create virtual environment and run"
    echo "  AUTOCOMPCHEM_BASE_URL=http://remote:8080 $0  # Use remote server"
    echo "  $0 --check-only                      # Just check server connectivity"
}

# Main execution
main() {
    local check_only=false
    local setup_only=false
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --check-only)
                check_only=true
                shift
                ;;
            --setup-only)
                setup_only=true
                shift
                ;;
            --no-venv)
                USE_VIRTUAL_ENV=false
                shift
                ;;
            --create-venv)
                CREATE_VIRTUAL_ENV=true
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
    
    # Check AutoCompChem server
    if ! check_autocompchem_server; then
        exit 1
    fi
    
    if [[ "$check_only" == "true" ]]; then
        echo "✅ Server check completed successfully"
        exit 0
    fi
    
    # Setup Python environment
    if ! setup_python_env; then
        exit 1
    fi
    
    if [[ "$setup_only" == "true" ]]; then
        echo "✅ Python environment setup completed"
        exit 0
    fi
    
    # Run MCP server
    run_mcp_server
}

# Trap Ctrl+C to provide clean exit message
trap 'echo ""; echo "🛑 AutoCompChem MCP Server stopped"; exit 0' INT

# Run main function
main "$@" 