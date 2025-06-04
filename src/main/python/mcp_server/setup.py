#!/usr/bin/env python3
"""
Setup script for AutoCompChem MCP Server
"""

from setuptools import setup, find_packages

setup(
    name="autocompchem-mcp-server",
    version="1.0.0",
    description="Model Context Protocol server for AutoCompChem computational chemistry tools",
    author="AutoCompChem Team",
    author_email="autocompchem@example.com",
    url="https://github.com/denoptim-project/AutoCompChem",
    packages=find_packages(),
    entry_points={
        'console_scripts': [
            'autocompchem-mcp-server=autocompchem_mcp_server:main',
        ],
    },
    install_requires=[
        "mcp>=1.2.0",
        "httpx>=0.25.0",
        "fastapi>=0.100.0",
        "pydantic>=2.0.0",
    ],
    python_requires=">=3.10",
    classifiers=[
        "Development Status :: 4 - Beta",
        "Intended Audience :: Science/Research",
        "License :: OSI Approved :: GNU Affero General Public License v3",
        "Programming Language :: Python :: 3",
        "Programming Language :: Python :: 3.10",
        "Programming Language :: Python :: 3.11",
        "Programming Language :: Python :: 3.12",
        "Topic :: Scientific/Engineering :: Chemistry",
        "Topic :: Software Development :: Libraries :: Python Modules",
    ],
) 