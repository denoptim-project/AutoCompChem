# AutoCompChem

![Anaconda_Version](https://anaconda.org/denoptim-project/autocompchem/badges/version.svg) ![Anaconda_last](https://anaconda.org/denoptim-project/autocompchem/badges/latest_release_date.svg) ![Anaconda_Platforms](https://anaconda.org/denoptim-project/autocompchem/badges/platforms.svg) ![Anaconda_License](https://anaconda.org/denoptim-project/autocompchem/badges/license.svg) ![Anaconda_downloads](
https://anaconda.org/denoptim-project/autocompchem/badges/downloads.svg)

AutoCompChem (or ACC) is a collection of tools used to automatize computational chemistry tasks.

## Installation

### Installation from Conda/Mamba
From within any conda/mamba environment you can install AutoCompChem as follows
```bash
conda install -c denoptim-project autocompchem
```
Now the `autocompchem` command should be available. Try to run the following to start using it:
```bash
autocompchem -h
```

### Installation from Source
See [dedicated instruction](doc/installation_from_source.md)

## Usage Modes

AutoCompChem provides a triple-mode approach that maintains full backward compatibility with traditional CLI usage while adding modern web capabilities and AI integration.

### CLI Mode (Traditional)
Run computational chemistry tasks from the command line using the `autocompchem` command (see  [Installation](#Installation)):

```bash
# Using the flat JAR (recommended for CLI)
autocompchem --help

# Run with parameters file
autocompchem my_parameters.txt

# Run specific task
autocompchem -t prepareInputGaussian [more args]

# Get task-specific help
autocompchem -t prepareInputGaussian -h
```

### Server Mode (REST API)
The server mode allows to start a backend web server that exposes the services of AutoCompChem for REST API access. Start the server with one of these alternatives depending on the intended usage:

```bash
# Production mode: starts server in background
./server-manager.sh start

# Basic usage: starts a server from a previously built installation
autocompchem --server

# Start server on custom port
autocompchem --server --server.port=9090

# Server with custom configuration
autocompchem --server --spring.profiles.active=production

# Using Maven (development)
mvn spring-boot:run -Dspring-boot.run.arguments="--server"

# Debug mode with various options
./debug-server.sh
```

**Server Management (Production)**:
```bash
./server-manager.sh start     # Start in background
./server-manager.sh stop      # Stop server  
./server-manager.sh status    # Check status
./server-manager.sh logs      # View logs
./server-manager.sh restart   # Restart server
./server-manager.sh version   # Show version info
./server-manager.sh health    # Check health endpoint
```

Once the server is running (default port: 8080):

- **Web Interface**: http://localhost:8080/
- **API Documentation**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/api/v1/autocompchem/health
- **Available Tasks**: http://localhost:8080/api/v1/autocompchem/tasks

### MCP Mode (Claude Integration)
The **Model Context Protocol (MCP)** integration allows Claude and other AI assistants to directly use AutoCompChem's computational chemistry tools through natural language conversations.

```bash
# Start the AutoCompChem server (required first)
./server-manager.sh start

# Launch the MCP server for Claude integration
./mcp-server-launcher.sh
```

**Features available to Claude:**
- 🧪 **Generate input files** for Gaussian, ORCA, NWChem, XTB, Spartan
- 📊 **Parse output files** from quantum chemistry calculations  
- 🔬 **Create molecules** from SMILES strings
- ⚙️ **Set up calculations** (geometry optimization, frequency analysis)
- 🎯 **Evaluate job quality** and suggest improvements
- 📋 **List available tasks** and get help for specific operations

**Quick Setup for Claude Desktop:**
1. Start AutoCompChem server: `./server-manager.sh start`
2. Install Python dependencies: `pip install -r src/main/python/mcp_server/requirements.txt`
3. Configure Claude Desktop (see [MCP Server Guide](doc/MCP_SERVER_GUIDE.md))
4. Ask Claude: *"What computational chemistry tasks are available?"*

**Example Claude Conversations:**
- *"Generate a Gaussian input file for water molecule optimization using B3LYP/6-31G*"*
- *"Convert the SMILES 'CCO' to a 3D structure and set up a frequency calculation"*
- *"Analyze my Gaussian output file and check if the calculation converged"*

See the [**detailed MCP integration guide**](doc/MCP_SERVER_GUIDE.md) for complete setup instructions and usage examples.

## Usage Instructions
See the [dedicated page](doc/usage_instructions.md)


## Acknowledgments
The Research Council of Norway (RCN) is acknowledged for financial support.
