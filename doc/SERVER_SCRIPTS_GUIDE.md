# AutoCompChem Server Scripts Guide

AutoCompChem provides two main server scripts for different use cases. This guide explains when and how to use each one.

## 📊 Server Scripts Overview

| Script | Purpose | Mode | Background | Process Management |
|--------|---------|------|------------|-------------------|
| `start-server.sh` | **Production** | Production profile | ✅ Yes | ✅ PID management |
| `debug-server.sh` | **Development** | Debug profile | ❌ Foreground | ❌ Manual control |

## 🚀 Production Server (`start-server.sh`)

**Best for**: Production deployments, MCP integration, long-running services, automated setups

### Features
- **Background execution** with proper process management
- **PID file** tracking for reliable start/stop operations
- **Production configuration** optimized for performance
- **Log file management** with rotation support
- **Health check integration** with automatic startup verification
- **Memory configuration** with production-optimized defaults
- **Graceful shutdown** with fallback to force-kill
- **Status monitoring** and process health checking
- **Dynamic version detection** from pom.xml (no hard-coding)
- **Intelligent JAR file discovery** with multiple fallback options
- **Smart process detection** (works for both foreground and background modes)
- **Flexible stop operations** (handles PID files and port-based detection)

### Usage Examples

```bash
# Basic operations
./start-server.sh start           # Start server in background
./start-server.sh stop            # Stop server gracefully
./start-server.sh restart         # Restart server
./start-server.sh status          # Check if running
./start-server.sh logs            # View logs (tail -f)
./start-server.sh health          # Test health endpoint
./start-server.sh version         # Show version information

# Configuration options
./start-server.sh start --port 9090                    # Custom port
./start-server.sh start --max-memory 4g --min-memory 1g # Memory tuning
./start-server.sh start --jar                          # Use JAR instead of Maven
./start-server.sh start --foreground                   # Run in foreground

# Environment variables
SERVER_PORT=8080 ./start-server.sh start              # Set port
MAX_MEMORY=4g MIN_MEMORY=1g ./start-server.sh start   # Set memory
```

### Configuration
- **Version Detection**: Automatically extracted from `pom.xml`
- **JAR Discovery**: Searches for multiple naming patterns:
  - `target/autocompchem-{version}.jar` (primary)
  - `target/autocompchem-{version}-jar-with-dependencies.jar`
  - `target/autocompchem.jar`
  - `autocompchem-{version}.jar`
  - `autocompchem.jar`
- **Default Port**: 8080
- **Memory**: 512m - 2048m (configurable)
- **Profile**: `production` (optimized for performance)
- **Logs**: `logs/autocompchem-server.log`
- **PID**: `logs/autocompchem-server.pid`

### Use Cases
- **MCP Integration**: Reliable background service for Claude integration
- **Production Deployments**: Server deployments and containerization
- **Automated Workflows**: CI/CD pipelines and automated testing
- **Long-running Services**: Computational chemistry workloads
- **System Integration**: Integration with other services and monitoring

## 🐛 Debug Server (`debug-server.sh`)

**Best for**: Development, debugging, testing, troubleshooting

### Features
- **Interactive mode** with multiple debug options
- **Enhanced logging** with configurable verbosity levels
- **Remote debugging** support (port 5005)
- **Performance profiling** with JVM monitoring
- **Trace mode** for detailed Spring Boot debugging
- **JAR debugging** for testing built artifacts
- **Real-time output** for immediate feedback

### Debug Modes

```bash
# Interactive mode (default)
./debug-server.sh                 # Choose mode interactively

# Direct mode selection
./debug-server.sh basic           # Basic debug with enhanced logging
./debug-server.sh remote          # Remote debugging (port 5005)
./debug-server.sh trace           # Full trace mode (verbose)
./debug-server.sh profile         # Performance profiling
./debug-server.sh jar             # Debug using JAR file
```

### Configuration
- **Default Port**: 8080
- **Memory**: 512m - 1024m (debug-optimized)
- **Profile**: `debug` (enhanced logging and debugging)
- **Logs**: Console output + `logs/autocompchem-debug.log`
- **Remote Debug Port**: 5005 (when enabled)

### Use Cases
- **Development**: Active development and code changes
- **Debugging**: Troubleshooting issues and step-through debugging
- **Testing**: Manual testing and validation
- **Learning**: Understanding system behavior
- **Troubleshooting**: Diagnosing problems and performance issues

## 🎯 When to Use Which Script

### Use `start-server.sh` when:
- ✅ Setting up **MCP integration** with Claude
- ✅ **Production deployments** or staging environments
- ✅ Need **background operation** with process management
- ✅ Running **automated workflows** or CI/CD
- ✅ **Long-running computations** that shouldn't be interrupted
- ✅ Need **reliable startup/shutdown** procedures
- ✅ **System integration** with monitoring tools

### Use `debug-server.sh` when:
- ✅ **Active development** and code changes
- ✅ **Debugging** application issues
- ✅ Need **remote debugging** with IDE
- ✅ **Testing** new features or configurations
- ✅ **Troubleshooting** performance or functionality issues
- ✅ **Learning** how AutoCompChem works
- ✅ Need **real-time console output**

## 🔧 MCP Integration Recommendations

For **Claude Desktop integration**, always use the **production server**:

```bash
# 1. Start AutoCompChem server
./start-server.sh start

# 2. Verify it's running
./start-server.sh status

# 3. Start MCP server for Claude
./mcp-server-launcher.sh
```

**Why production server for MCP?**
- **Stability**: Background operation prevents accidental termination
- **Performance**: Production-optimized configuration
- **Reliability**: Proper process management and health checks
- **Logging**: Structured logs for troubleshooting
- **Memory**: Better memory management for computational tasks

## 📋 Quick Reference

### Production Server Commands
```bash
./start-server.sh start          # Start
./start-server.sh stop           # Stop  
./start-server.sh restart        # Restart
./start-server.sh status         # Status
./start-server.sh logs           # View logs
./start-server.sh health         # Health check
./start-server.sh version        # Show version information
```

### Debug Server Commands
```bash
./debug-server.sh               # Interactive
./debug-server.sh basic         # Basic debug
./debug-server.sh remote        # Remote debug
./debug-server.sh trace         # Verbose
```

### Server Health Check
```bash
# Direct curl
curl http://localhost:8080/api/v1/autocompchem/health

# Using production script
./start-server.sh health

# Check if running
./start-server.sh status
```

## 🚨 Troubleshooting

### Common Issues

**1. Status Detection**
```bash
# The status command works for both modes:
./start-server.sh status

# Example outputs:
# Background: "Mode: background", "Control: Use './start-server.sh stop' to stop"
# Foreground: "Mode: foreground", "Control: Running in foreground (use Ctrl+C to stop)"
```

**2. Port Already in Use**
```bash
# Check what's using the port
lsof -i :8080

# Use different port
./start-server.sh start --port 9090
```

**3. Server Won't Start**
```bash
# Check logs
./start-server.sh logs

# Try debug mode for more info
./debug-server.sh basic
```

**4. Process Management Issues**
```bash
# Force stop if needed
pkill -f autocompchem

# Clean up PID file
rm -f logs/autocompchem-server.pid

# Fresh start
./start-server.sh start
```

**5. Memory Issues**
```bash
# Increase memory
./start-server.sh start --max-memory 4g

# Check memory usage
./start-server.sh status
```

### Debug vs Production Comparison

| Aspect | Production (`start-server.sh`) | Debug (`debug-server.sh`) |
|--------|--------------------------------|---------------------------|
| **Process** | Background daemon | Foreground process |
| **Control** | start/stop/restart commands | Manual Ctrl+C |
| **Logging** | File-based | Console + file |
| **Memory** | Optimized (2GB max) | Conservative (1GB max) |
| **Profile** | `production` | `debug` |
| **Monitoring** | Health checks | Manual observation |
| **IDE Integration** | Limited | Full remote debugging |
| **Automation** | Fully automated | Manual operation |

## 📚 Related Documentation

- [MCP Server Guide](MCP_SERVER_GUIDE.md) - Complete MCP integration setup
- [Usage Instructions](usage_instructions.md) - General AutoCompChem usage
- [Installation Guide](installation_from_source.md) - Installation from source

---

*Choose the right tool for the job: production server for deployment and MCP integration, debug server for development and troubleshooting.* 