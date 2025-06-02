# AutoCompChem Server Debug Guide

This guide provides step-by-step instructions for debugging the AutoCompChem Spring Boot server.

## Quick Start

### 1. Interactive Debug Launcher
```bash
./debug-server.sh
```
This will show you a menu with different debug options.

### 2. Direct Debug Modes
```bash
./debug-server.sh basic     # Basic debug mode
./debug-server.sh remote    # Remote debugging
./debug-server.sh trace     # Verbose trace mode
./debug-server.sh profile   # Performance profiling
./debug-server.sh jar       # Debug using JAR file
```

## Debug Modes Explained

### 🐛 Basic Debug Mode
- **Purpose**: Enhanced logging without remote debugging
- **Use case**: Troubleshooting application logic and flow
- **Features**: 
  - DEBUG level logging for AutoCompChem components
  - Enhanced Spring Web logging
  - All actuator endpoints enabled

**Start:**
```bash
./debug-server.sh basic
```

**Endpoints to check:**
- http://localhost:8080/actuator/health
- http://localhost:8080/actuator/loggers
- http://localhost:8080/actuator/beans

### 🔌 Remote Debug Mode
- **Purpose**: Connect IDE debugger to running application
- **Use case**: Step-through debugging, breakpoints, variable inspection
- **Port**: 5005 (configurable)

**Start:**
```bash
./debug-server.sh remote
```

**IDE Setup:**

#### IntelliJ IDEA
1. Run → Edit Configurations
2. Click `+` → Remote JVM Debug
3. Set Host: `localhost`, Port: `5005`
4. Click "Debug" to attach

#### VS Code
1. Add to `.vscode/launch.json`:
```json
{
    "type": "java",
    "name": "Debug AutoCompChem Remote",
    "request": "attach",
    "hostName": "localhost",
    "port": 5005
}
```

#### Eclipse
1. Debug Configurations → Remote Java Application
2. Host: `localhost`, Port: `5005`

### 📊 Trace Mode
- **Purpose**: Maximum logging detail
- **Use case**: Deep troubleshooting, framework issues
- **Warning**: Generates large amounts of logs

**Start:**
```bash
./debug-server.sh trace
```

### 📈 Profile Mode
- **Purpose**: Performance analysis
- **Use case**: Memory leaks, GC analysis, performance bottlenecks
- **Features**: GC logging, heap dumps

**Start:**
```bash
./debug-server.sh profile
```

## Manual Debug Commands

### Maven with Debug
```bash
mvn spring-boot:run \
    -Dspring-boot.run.arguments="--server --spring.profiles.active=debug" \
    -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

### JAR with Debug
```bash
java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005 \
     -Dspring.profiles.active=debug \
     -jar target/autocompchem-3.0.0.jar --server
```

### CLI with Debug
```bash
java -Dspring.profiles.active=debug \
     -jar target/autocompchem-3.0.0.jar --help
```

## Debug Configuration Files

### `application-debug.yml`
Contains debug-specific configuration:
- Enhanced logging levels
- All actuator endpoints enabled
- Request/response logging
- Exception details

### `log4j2.xml`
Logging configuration with:
- Console and file appenders
- Different patterns for CLI vs Server
- Rotating file logs

## Useful Debug Endpoints

Once the server is running in debug mode:

### Health and Status
```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/info
```

### Logging Control
```bash
# View current log levels
curl http://localhost:8080/actuator/loggers

# Change log level at runtime
curl -X POST http://localhost:8080/actuator/loggers/autocompchem.service \
     -H "Content-Type: application/json" \
     -d '{"configuredLevel": "TRACE"}'
```

### Application Inspection
```bash
# View all beans
curl http://localhost:8080/actuator/beans

# View environment
curl http://localhost:8080/actuator/env

# View configuration properties
curl http://localhost:8080/actuator/configprops
```

### API Testing
```bash
# Test API endpoints with debug info
curl -v http://localhost:8080/api/v1/autocompchem/tasks
curl -v http://localhost:8080/api/v1/autocompchem/info
```

## Log Files

Debug logs are written to:
- **Console**: Real-time debug output
- **File**: `logs/autocompchem-debug.log`
- **GC Logs** (profile mode): `logs/gc.log`

### Monitoring Logs
```bash
# Follow debug log in real-time
tail -f logs/autocompchem-debug.log

# Search for specific patterns
grep "ERROR" logs/autocompchem-debug.log
grep "autocompchem.service" logs/autocompchem-debug.log
```

## Common Debug Scenarios

### 1. API Request Issues
- Start with `basic` mode
- Check request/response logging
- Examine controller and service logs

### 2. Spring Configuration Problems
- Use `trace` mode
- Check actuator/beans endpoint
- Review auto-configuration logs

### 3. Performance Issues
- Use `profile` mode
- Monitor GC logs
- Check heap usage

### 4. Step-through Debugging
- Use `remote` mode
- Set breakpoints in IDE
- Inspect variables and call stack

## Troubleshooting

### Debug Port Already in Use
```bash
# Find process using port 5005
lsof -i :5005

# Kill process if needed
kill -9 <PID>
```

### Out of Memory
Increase heap size in debug scripts:
```bash
-Xms1024m -Xmx2048m
```

### Too Many Logs
Reduce logging levels:
```bash
--logging.level.org.springframework=INFO
--logging.level.autocompchem=INFO
```

## Tips

- 📝 Always check logs first: `tail -f logs/autocompchem-debug.log`
- 🔍 Use actuator endpoints for runtime inspection
- 🐛 Set breakpoints at controller entry points
- 📊 Monitor memory usage during long-running tests
- 🚀 Use `basic` mode for most debugging tasks
- 🔌 Use `remote` mode only when you need step-through debugging 