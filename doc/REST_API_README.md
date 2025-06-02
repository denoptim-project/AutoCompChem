# 📚 AutoCompChem REST API

This guide documents the Spring Boot REST API that has been added to AutoCompChem, providing a modern web interface for computational chemistry tasks automation.

## Interactive Documentation
Visit http://localhost:8080/swagger-ui.html for interactive API documentation where you can:
- Browse all available endpoints
- Test API calls directly from your browser
- View request/response schemas
- Download OpenAPI specification

## Main API Endpoints

### Core AutoCompChem Operations
Sorry: :construction:  TODO...
```http
GET    /api/v1/autocompchem/info           # API information
GET    /api/v1/autocompchem/tasks          # Available computational tasks
GET    /api/v1/autocompchem/tasks/{task}/help  # Help for specific task
POST   /api/v1/autocompchem/tasks/{task}/execute  # Execute a task
GET    /api/v1/autocompchem/capabilities   # Worker capabilities
GET    /api/v1/autocompchem/health         # Health check
```

### Molecular Operations
Sorry: :construction:  TODO...
```http
```

#### Computational Chemistry Software Integration
Sorry: :construction:  TODO...
```http
GET    /api/v1/compchem/software           # Supported software packages
POST   /api/v1/compchem/software/{software}/input   # Generate input files
POST   /api/v1/compchem/software/{software}/output  # Parse output files
POST   /api/v1/compchem/job/evaluate       # Evaluate job results
GET    /api/v1/compchem/job/evaluation     # Job evaluation info
```

## 🔧 Configuration

The API server can be configured via `src/main/resources/application.yml`:

```yaml
server:
  port: 8080                    # Change server port
spring:
  servlet:
    multipart:
      max-file-size: 100MB      # Max upload file size
      max-request-size: 100MB   # Max request size
```

## 🧪 Example Usage

### 1. Generate Gaussian Input File

```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{
    "inputFile": "molecule.sdf",
    "outputFile": "gaussian.com",
    "method": "B3LYP",
    "basisSet": "6-31G*"
  }' \
  http://localhost:8080/api/v1/compchem/software/gaussian/input
```

### 3. Execute Custom Task

```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{
    "inputFile": "input.sdf",
    "outputFile": "output.sdf"
  }' \
  http://localhost:8080/api/v1/autocompchem/tasks/readAtomContainers/execute
```


## 🔍 Supported Features

### Available Tasks
Sorry: :construction:  TODO...

### File Formats
Sorry: :construction:  TODO...


## 🔒 Security Considerations

Current implementation is designed for local/trusted environments. For production deployment, consider adding:
- Authentication/authorization
- Rate limiting
- Input validation
- HTTPS/TLS encryption
- CORS configuration

## 🐛 Troubleshooting

### Common Issues

1. **Port already in use:**
   ```bash
   # Change port in application.yml or use:
   mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dserver.port=8081"
   ```

2. **File upload too large:**
   ```bash
   # Increase limits in application.yml:
   spring.servlet.multipart.max-file-size: 500MB
   ```

3. **Memory issues with large files:**
   ```bash
   # Increase JVM memory:
   export MAVEN_OPTS="-Xmx4g"
   mvn spring-boot:run
   ```

### Logs
Application logs are written to:
- Console output
- `logs/autocompchem-api.log` file


## 📝 API Response Formats

All API responses use JSON format:

```json
{
  "status": "success|error",
  "data": { /* response data */ },
  "message": "Human readable message"
}
```

Error responses include detailed error information:
```json
{
  "status": "error",
  "message": "Detailed error description",
  "timestamp": "2024-01-01T12:00:00Z"
}
```

## 🤝 Contributing

To extend the REST API:
1. Add new endpoints to existing controllers or create new controllers
2. Extend the `AutoCompChemService` class for business logic
3. Update the OpenAPI documentation with `@Operation` annotations
4. Add tests for new functionality

## 📖 Additional Resources

- **AutoCompChem Documentation**: [usage instructions](usage_instructions.md)
- **Spring Boot Reference**: https://spring.io/projects/spring-boot
- **OpenAPI Specification**: https://swagger.io/specification/
- **Swagger UI Documentation**: https://swagger.io/tools/swagger-ui/ 
