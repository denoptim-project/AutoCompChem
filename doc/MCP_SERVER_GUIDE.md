# AutoCompChem MCP Server Guide

This guide explains how to set up and use the AutoCompChem Model Context Protocol (MCP) server to integrate AutoCompChem computational chemistry tools with Claude and other MCP clients.

## What is MCP?

The **Model Context Protocol (MCP)** is an open standard by Anthropic that enables seamless integration between AI assistants like Claude and external data sources/tools. It allows Claude to directly interact with AutoCompChem's computational chemistry capabilities through a standardized protocol.

## Features

The AutoCompChem MCP server provides Claude with access to:

### 🧪 **Core Computational Chemistry Tools**
- **Input File Generation**: Create input files for Gaussian, ORCA, NWChem, XTB, and Spartan
- **Output File Parsing**: Read and analyze output files from quantum chemistry calculations
- **Job Evaluation**: Assess calculation quality and suggest improvements
- **Task Execution**: Run any available AutoCompChem task with custom parameters

### 🔬 **Molecular Structure Tools**
- **SMILES to 3D**: Convert SMILES strings to 3D molecular structures
- **Geometry Optimization**: Set up geometry optimization calculations
- **Frequency Analysis**: Configure vibrational frequency calculations
- **Molecular Properties**: Calculate molecular properties and descriptors

### 📊 **Information and Discovery**
- **Available Tasks**: List all computational chemistry tasks
- **Task Help**: Get detailed help for specific tasks
- **Software Support**: Check supported quantum chemistry packages
- **API Status**: Monitor AutoCompChem server health

## Installation

### Prerequisites

1. **AutoCompChem Server**: Ensure the AutoCompChem REST API server is running (default: `http://localhost:8080`)
2. **Python 3.10+**: Required for the MCP server
3. **Claude Desktop**: For testing the integration

### Setup Steps

1. **Start AutoCompChem Server**:
   ```bash
   # Production mode (recommended for MCP integration)
   ./server-manager.sh start
   
   # Development mode (for debugging)
   ./debug-server.sh
   
   # Verify server is running
   curl http://localhost:8080/api/v1/autocompchem/health
   ```

2. **Install Python Dependencies**:
   ```bash
   cd src/main/python/mcp_server
   pip install -r requirements.txt
   ```

3. **Configure Environment** (optional):
   ```bash
   export AUTOCOMPCHEM_BASE_URL="http://localhost:8080"  # Default
   ```

4. **Test the MCP Server**:
   ```bash
   python autocompchem_mcp_server.py
   ```

## AutoCompChem Server Management

### Production Server Script

The **`server-manager.sh`** script provides production-ready server management:

```bash
# Start server in background
./server-manager.sh start

# Stop server
./server-manager.sh stop

# Restart server
./server-manager.sh restart

# Check server status
./server-manager.sh status

# View logs
./server-manager.sh logs

# Check health endpoint
./server-manager.sh health

# Start on custom port
./server-manager.sh start --port 9090

# Start with more memory
./server-manager.sh start --max-memory 4g --min-memory 1g
```

### Development Server Script

For development and debugging, use **`debug-server.sh`**:

```bash
# Interactive debug mode selection
./debug-server.sh

# Basic debug mode
./debug-server.sh basic

# Remote debugging (port 5005)
./debug-server.sh remote
```

## Claude Desktop Integration

### 1. Configure Claude Desktop

Edit your Claude Desktop configuration file:
- **macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
- **Windows**: `%APPDATA%\Claude\claude_desktop_config.json`

Add the AutoCompChem MCP server:

```json
{
    "mcpServers": {
        "autocompchem": {
            "command": "python",
            "args": [
                "/ABSOLUTE/PATH/TO/AutoCompChem/src/main/python/mcp_server/autocompchem_mcp_server.py"
            ],
            "env": {
                "AUTOCOMPCHEM_BASE_URL": "http://localhost:8080"
            }
        }
    }
}
```

### 2. Restart Claude Desktop

After updating the configuration, restart Claude Desktop completely.

### 3. Verify Integration

In Claude Desktop, you should see the AutoCompChem tools available. Look for the tools icon (🔧) in the interface.

## Usage Examples

### Example 1: Get Available Tasks
**Claude Prompt**: "What computational chemistry tasks are available in AutoCompChem?"

### Example 2: Generate Gaussian Input File
**Claude Prompt**: "Create a Gaussian input file for water molecule geometry optimization using B3LYP/6-31G*"

### Example 3: Set Up Frequency Calculation
**Claude Prompt**: "Set up a frequency calculation for benzene.xyz using ORCA with M06-2X/def2-TZVP"

### Example 4: Create Molecule from SMILES
**Claude Prompt**: "Convert the SMILES string 'CCO' (ethanol) to a 3D XYZ structure"

### Example 5: Evaluate Calculation Results
**Claude Prompt**: "Evaluate the quality of my Gaussian calculation output file at /path/to/calculation.out"

## Available MCP Tools

| Tool Name | Description | Example Usage |
|-----------|-------------|---------------|
| `get_api_info` | Get AutoCompChem API information | "What's the status of AutoCompChem?" |
| `list_available_tasks` | List all computational tasks | "What tasks are available?" |
| `get_task_help` | Get help for specific task | "How do I use prepareInputGaussian?" |
| `execute_task` | Execute any AutoCompChem task | "Run the readGaussianOutput task" |
| `get_supported_software` | List supported QC software | "What quantum chemistry software is supported?" |
| `generate_input_file` | Create QC input files | "Generate ORCA input for water optimization" |
| `read_output_file` | Parse QC output files | "Analyze my Gaussian output file" |
| `evaluate_job` | Evaluate calculation quality | "Check if my calculation converged properly" |
| `create_molecule_from_smiles` | SMILES to 3D structure | "Convert 'c1ccccc1' to benzene.xyz" |
| `optimize_geometry` | Set up geometry optimization | "Optimize methanol with B3LYP/6-31G*" |
| `calculate_frequencies` | Set up frequency analysis | "Calculate frequencies for optimized water" |

## Parameter Formats

### JSON Parameter Examples

**For input file generation**:
```json
{
    "INFILE": "/path/to/molecule.xyz",
    "OUTFILE": "/path/to/input.inp",
    "JOBTYPE": "OPT",
    "METHOD": "B3LYP",
    "BASISSET": "6-31G*",
    "CHARGE": "0",
    "SPINMULT": "1"
}
```

**For output file reading**:
```json
{
    "INFILE": "/path/to/calculation.out",
    "ANALYSIS": "LASTGEOMETRY,ENERGY,FREQUENCIES"
}
```

## Troubleshooting

### Common Issues

**1. MCP Server Not Showing in Claude**
- Check your `claude_desktop_config.json` syntax
- Ensure absolute paths are used
- Restart Claude Desktop completely
- Check Claude's logs: `~/Library/Logs/Claude/mcp*.log`

**2. AutoCompChem Connection Errors**
- Verify AutoCompChem server is running: `curl http://localhost:8080/api/v1/autocompchem/health`
- Check the `AUTOCOMPCHEM_BASE_URL` environment variable
- Ensure network connectivity between MCP server and AutoCompChem

**3. Tool Execution Failures**
- Verify JSON parameter format
- Check file paths exist and are accessible
- Review AutoCompChem server logs for detailed errors

### Debug Mode

Enable debug logging:
```bash
export PYTHONPATH=/path/to/autocompchem/mcp_server
python -c "
import logging
logging.basicConfig(level=logging.DEBUG)
from autocompchem_mcp_server import *
"
```

### Logs and Monitoring

**Claude Desktop Logs**:
```bash
# macOS
tail -f ~/Library/Logs/Claude/mcp*.log

# Check specific server logs
tail -f ~/Library/Logs/Claude/mcp-server-autocompchem.log
```

**AutoCompChem Server Logs**:
```bash
# If running via Maven
tail -f logs/autocompchem-debug.log

# Check server status
curl http://localhost:8080/api/v1/autocompchem/health
```

## Advanced Configuration

### Custom Server Location

If running AutoCompChem on a different host/port:

```json
{
    "mcpServers": {
        "autocompchem": {
            "command": "python",
            "args": ["/path/to/autocompchem_mcp_server.py"],
            "env": {
                "AUTOCOMPCHEM_BASE_URL": "http://remote-server:8080"
            }
        }
    }
}
```

### Multiple AutoCompChem Instances

Configure multiple servers:

```json
{
    "mcpServers": {
        "autocompchem-local": {
            "command": "python",
            "args": ["/path/to/autocompchem_mcp_server.py"],
            "env": {
                "AUTOCOMPCHEM_BASE_URL": "http://localhost:8080"
            }
        },
        "autocompchem-cluster": {
            "command": "python", 
            "args": ["/path/to/autocompchem_mcp_server.py"],
            "env": {
                "AUTOCOMPCHEM_BASE_URL": "http://cluster.example.com:8080"
            }
        }
    }
}
```

## Integration Examples

### Research Workflow with Claude

**1. Molecule Design**: "I need to design a drug molecule that inhibits protein X. Start with the SMILES 'CC(=O)Nc1ccc(O)cc1' and optimize it with DFT."

**2. Conformational Analysis**: "Generate multiple conformers for this flexible molecule and find the lowest energy structure."

**3. Property Prediction**: "Calculate the vibrational frequencies and thermochemical properties for the optimized structure."

**4. Results Analysis**: "Analyze the calculation results and suggest if the convergence criteria were met."

### Automated Calculation Setup

Claude can help set up complex calculation workflows:

```
"Set up a complete DFT workflow for caffeine:
1. Convert SMILES to 3D structure
2. Optimize geometry with B3LYP/6-31G*
3. Calculate frequencies at the same level
4. Prepare input for higher-level single-point energy with M06-2X/def2-TZVP"
```

## Security Considerations

### File System Access
- The MCP server can read/write files accessible to the Python process
- Use appropriate file permissions and working directories
- Consider sandboxing for production environments

### Network Security
- AutoCompChem API communication is HTTP by default
- Consider HTTPS/TLS for production deployments
- Implement authentication if exposing to external networks

### Resource Management
- Computational chemistry tasks can be resource-intensive
- Monitor CPU/memory usage during large calculations
- Implement timeouts for long-running operations (currently 5 minutes)

## Contributing

### Adding New Tools

To extend the MCP server with new tools:

1. Add a new `@mcp.tool()` decorated function
2. Define clear parameter types and documentation
3. Handle errors gracefully
4. Update this documentation

Example:
```python
@mcp.tool()
async def my_new_tool(parameter1: str, parameter2: int = 5) -> str:
    """Description of what this tool does.
    
    Args:
        parameter1: Description of parameter1
        parameter2: Description of parameter2 (default: 5)
    """
    # Implementation here
    return "Result"
```

### Testing

Test the MCP server with different scenarios:
- Valid and invalid parameters
- Network connectivity issues
- Large file handling
- Error conditions

## Resources

- **MCP Documentation**: https://modelcontextprotocol.io/
- **AutoCompChem Documentation**: [../doc/usage_instructions.md](usage_instructions.md)
- **Claude Desktop**: https://claude.ai/download
- **Python MCP SDK**: https://github.com/modelcontextprotocol/python-sdk

## Support

For issues and questions:
- **AutoCompChem Issues**: GitHub repository issues
- **MCP General Support**: MCP GitHub repository  
- **Claude Desktop Support**: Anthropic support channels

---

*This integration brings the power of computational chemistry directly to your conversations with Claude, making complex molecular calculations as easy as asking a question.* 