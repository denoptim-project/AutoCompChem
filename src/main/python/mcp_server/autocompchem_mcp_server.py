#!/usr/bin/env python3
"""
AutoCompChem MCP Server

A Model Context Protocol server that exposes AutoCompChem computational chemistry 
functionality to Claude and other MCP clients.

This server provides tools for:
- Generating input files for quantum chemistry software (Gaussian, ORCA, NWChem, etc.)
- Reading and parsing output files from computational chemistry software
- Evaluating computational chemistry jobs
- Managing molecular structures and geometries
- Running computational chemistry tasks

Usage:
    python autocompchem_mcp_server.py

Author: AutoCompChem Team
"""

import asyncio
import json
import logging
import os
import tempfile
from pathlib import Path
from typing import Any, Dict, List, Optional

import httpx
from mcp.server.fastmcp import FastMCP
from mcp.types import TextContent, Tool

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Initialize FastMCP server
mcp = FastMCP("autocompchem")

# Configuration
AUTOCOMPCHEM_BASE_URL = os.getenv("AUTOCOMPCHEM_BASE_URL", "http://localhost:8080")
DEFAULT_TIMEOUT = 300.0  # 5 minutes for computational chemistry tasks

class AutoCompChemClient:
    """HTTP client for communicating with AutoCompChem REST API"""
    
    def __init__(self, base_url: str = AUTOCOMPCHEM_BASE_URL):
        self.base_url = base_url.rstrip('/')
        
    async def make_request(self, method: str, endpoint: str, data: Optional[Dict] = None) -> Dict[str, Any]:
        """Make HTTP request to AutoCompChem API"""
        url = f"{self.base_url}{endpoint}"
        
        async with httpx.AsyncClient(timeout=DEFAULT_TIMEOUT) as client:
            try:
                if method.upper() == "GET":
                    response = await client.get(url)
                elif method.upper() == "POST":
                    response = await client.post(url, json=data or {})
                else:
                    raise ValueError(f"Unsupported HTTP method: {method}")
                
                response.raise_for_status()
                return response.json()
                
            except httpx.RequestError as e:
                logger.error(f"Request failed: {e}")
                raise Exception(f"Failed to connect to AutoCompChem server at {url}: {e}")
            except httpx.HTTPStatusError as e:
                logger.error(f"HTTP error {e.response.status_code}: {e.response.text}")
                raise Exception(f"AutoCompChem API error {e.response.status_code}: {e.response.text}")

# Initialize client
acc_client = AutoCompChemClient()

@mcp.tool()
async def get_api_info() -> str:
    """Get information about the AutoCompChem API and available capabilities."""
    try:
        info = await acc_client.make_request("GET", "/api/v1/autocompchem/info")
        health = await acc_client.make_request("GET", "/api/v1/autocompchem/health")
        
        return f"""
AutoCompChem API Information:
- Name: {info.get('name', 'N/A')}
- Version: {info.get('version', 'N/A')}
- Description: {info.get('description', 'N/A')}
- Status: {health.get('status', 'N/A')}
- Service: {health.get('service', 'N/A')}
"""
    except Exception as e:
        return f"Error getting API info: {str(e)}"

@mcp.tool()
async def list_available_tasks() -> str:
    """Get a list of all available computational chemistry tasks."""
    try:
        tasks = await acc_client.make_request("GET", "/api/v1/autocompchem/tasks")
        
        if isinstance(tasks, list):
            formatted_tasks = "\n".join([f"- {task}" for task in tasks])
            return f"Available AutoCompChem tasks:\n{formatted_tasks}"
        else:
            return f"Available tasks: {tasks}"
            
    except Exception as e:
        return f"Error getting tasks: {str(e)}"

@mcp.tool()
async def get_task_help(task_name: str) -> str:
    """Get detailed help information for a specific computational chemistry task.
    
    Args:
        task_name: Name of the task to get help for (e.g., 'prepareInputGaussian', 'readGaussianOutput')
    """
    try:
        help_text = await acc_client.make_request("GET", f"/api/v1/autocompchem/tasks/{task_name}/help")
        return f"Help for task '{task_name}':\n{help_text}"
        
    except Exception as e:
        return f"Error getting help for task '{task_name}': {str(e)}"

@mcp.tool()
async def execute_task(task_name: str, parameters: str) -> str:
    """Execute a computational chemistry task with specified parameters.
    
    Args:
        task_name: Name of the task to execute (e.g., 'prepareInputGaussian', 'readGaussianOutput')
        parameters: JSON string containing task parameters (e.g., '{"INFILE": "/path/to/input.xyz", "OUTFILE": "/path/to/output.inp"}')
    """
    try:
        # Parse parameters
        try:
            params = json.loads(parameters)
        except json.JSONDecodeError as e:
            return f"Error parsing parameters JSON: {e}"
        
        # Execute task
        result = await acc_client.make_request("POST", f"/api/v1/autocompchem/tasks/{task_name}/execute", params)
        
        # Format result
        status = result.get('status', 'Unknown')
        exposed_data = result.get('exposedData', {})
        exposed_count = result.get('exposedDataCount', 0)
        
        response = f"Task '{task_name}' execution result:\n"
        response += f"Status: {status}\n"
        response += f"Exposed data items: {exposed_count}\n"
        
        if exposed_data:
            response += "\nExposed data:\n"
            for key, value in exposed_data.items():
                response += f"- {key}: {str(value)[:200]}{'...' if len(str(value)) > 200 else ''}\n"
        
        return response
        
    except Exception as e:
        return f"Error executing task '{task_name}': {str(e)}"

@mcp.tool()
async def get_supported_software() -> str:
    """Get information about supported computational chemistry software packages."""
    try:
        software = await acc_client.make_request("GET", "/api/v1/compchem/software")
        
        input_writers = software.get('inputWriters', {})
        output_readers = software.get('outputReaders', {})
        
        response = "Supported Computational Chemistry Software:\n\n"
        
        response += "Input File Generators:\n"
        for software_name, task_name in input_writers.items():
            response += f"- {software_name.upper()}: {task_name}\n"
        
        response += "\nOutput File Readers:\n"
        for software_name, task_name in output_readers.items():
            response += f"- {software_name.upper()}: {task_name}\n"
        
        return response
        
    except Exception as e:
        return f"Error getting supported software: {str(e)}"

@mcp.tool()
async def generate_input_file(software: str, parameters: str, working_directory: Optional[str] = None) -> str:
    """Generate input file for computational chemistry software.
    
    Args:
        software: Software name (gaussian, orca, nwchem, xtb, spartan)
        parameters: JSON string with parameters like molecular geometry, calculation type, etc.
        working_directory: Optional working directory for the calculation
    """
    try:
        # Parse parameters
        try:
            params = json.loads(parameters)
        except json.JSONDecodeError as e:
            return f"Error parsing parameters JSON: {e}"
        
        # Add working directory if specified
        if working_directory:
            params['WORKDIR'] = working_directory
        
        # Generate input file
        result = await acc_client.make_request("POST", f"/api/v1/compchem/software/{software}/input", params)
        
        return f"Input file generation for {software.upper()}:\n{json.dumps(result, indent=2)}"
        
    except Exception as e:
        return f"Error generating {software} input file: {str(e)}"

@mcp.tool()
async def read_output_file(software: str, parameters: str) -> str:
    """Parse output file from computational chemistry software.
    
    Args:
        software: Software name (gaussian, orca, nwchem, xtb, spartan)
        parameters: JSON string with parameters including output file path and analysis options
    """
    try:
        # Parse parameters
        try:
            params = json.loads(parameters)
        except json.JSONDecodeError as e:
            return f"Error parsing parameters JSON: {e}"
        
        # Read output file
        result = await acc_client.make_request("POST", f"/api/v1/compchem/software/{software}/output", params)
        
        return f"Output file analysis for {software.upper()}:\n{json.dumps(result, indent=2)}"
        
    except Exception as e:
        return f"Error reading {software} output file: {str(e)}"

@mcp.tool()
async def evaluate_job(parameters: str) -> str:
    """Evaluate a computational chemistry job to assess its quality and suggest improvements.
    
    Args:
        parameters: JSON string with job evaluation parameters (e.g., output file paths, criteria)
    """
    try:
        # Parse parameters
        try:
            params = json.loads(parameters)
        except json.JSONDecodeError as e:
            return f"Error parsing parameters JSON: {e}"
        
        # Evaluate job
        result = await acc_client.make_request("POST", "/api/v1/compchem/job/evaluate", params)
        
        return f"Job evaluation result:\n{json.dumps(result, indent=2)}"
        
    except Exception as e:
        return f"Error evaluating job: {str(e)}"

@mcp.tool()
async def create_molecule_from_smiles(smiles: str, output_format: str = "XYZ") -> str:
    """Create a 3D molecular structure from a SMILES string.
    
    Args:
        smiles: SMILES string representation of the molecule
        output_format: Output format for the molecular structure (XYZ, SDF, etc.)
    """
    try:
        # Create temporary working directory
        with tempfile.TemporaryDirectory() as tmpdir:
            params = {
                "SMILES": smiles,
                "OUTFORMAT": output_format,
                "WORKDIR": tmpdir
            }
            
            # Use a molecular structure generation task if available
            result = await acc_client.make_request("POST", "/api/v1/autocompchem/tasks/createMoleculeFromSMILES/execute", params)
            
            return f"Molecule creation from SMILES '{smiles}':\n{json.dumps(result, indent=2)}"
            
    except Exception as e:
        return f"Error creating molecule from SMILES '{smiles}': {str(e)}"

@mcp.tool()
async def optimize_geometry(input_file: str, software: str = "gaussian", method: str = "B3LYP", basis_set: str = "6-31G*") -> str:
    """Perform geometry optimization of a molecular structure.
    
    Args:
        input_file: Path to input molecular structure file
        software: Computational chemistry software to use (gaussian, orca, etc.)
        method: DFT method or level of theory (e.g., B3LYP, M06-2X)
        basis_set: Basis set for the calculation (e.g., 6-31G*, def2-TZVP)
    """
    try:
        with tempfile.TemporaryDirectory() as tmpdir:
            params = {
                "INFILE": input_file,
                "JOBTYPE": "OPT",
                "METHOD": method,
                "BASISSET": basis_set,
                "WORKDIR": tmpdir
            }
            
            # Generate input file
            input_result = await acc_client.make_request("POST", f"/api/v1/compchem/software/{software}/input", params)
            
            return f"Geometry optimization setup for {software.upper()}:\n{json.dumps(input_result, indent=2)}\n\nNote: This creates the input file. You'll need to run the calculation with the quantum chemistry software and then parse the output."
            
    except Exception as e:
        return f"Error setting up geometry optimization: {str(e)}"

@mcp.tool()
async def calculate_frequencies(input_file: str, software: str = "gaussian", method: str = "B3LYP", basis_set: str = "6-31G*") -> str:
    """Set up a frequency calculation to compute vibrational frequencies and thermochemical properties.
    
    Args:
        input_file: Path to input molecular structure file (should be an optimized geometry)
        software: Computational chemistry software to use
        method: DFT method or level of theory
        basis_set: Basis set for the calculation
    """
    try:
        with tempfile.TemporaryDirectory() as tmpdir:
            params = {
                "INFILE": input_file,
                "JOBTYPE": "FREQ",
                "METHOD": method,
                "BASISSET": basis_set,
                "WORKDIR": tmpdir
            }
            
            # Generate input file
            input_result = await acc_client.make_request("POST", f"/api/v1/compchem/software/{software}/input", params)
            
            return f"Frequency calculation setup for {software.upper()}:\n{json.dumps(input_result, indent=2)}\n\nNote: This creates the input file for frequency analysis. Run the calculation and parse output to get vibrational frequencies, zero-point energy, and thermochemical data."
            
    except Exception as e:
        return f"Error setting up frequency calculation: {str(e)}"

if __name__ == "__main__":
    # Run the MCP server
    mcp.run(transport='stdio') 