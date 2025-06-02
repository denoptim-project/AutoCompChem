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

AutoCompChem provides a dual-mode approach that maintains full backward compatibility with traditional CLI usage while adding modern REST API capabilities.

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
The server mode allows to start a backend web server that exposes the services of AutoCompChem for HTTP API access:

```bash
# Using either JAR
autocompchem --server

# Start server on custom port
autocompchem --server --server.port=9090

# Server with custom configuration
autocompchem --server --spring.profiles.active=production
```

Once the server is running (default port: 8080):

- **Web Interface**: http://localhost:8080/
- **API Documentation**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/api/v1/autocompchem/health
- **Available Tasks**: http://localhost:8080/api/v1/autocompchem/tasks


## Usage Instructions
See the [dedicated page](doc/usage_instructions.md)


## Acknowledgments
The Research Council of Norway (RCN) is acknowledged for financial support.
