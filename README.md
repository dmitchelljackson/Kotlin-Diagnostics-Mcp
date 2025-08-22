# Kotlin Diagnostics MCP

> **⚠️ WORK IN PROGRESS ⚠️**
>
> This project is currently under active development. Installation steps, APIs, and functionality may change rapidly
> without notice. Use at your own risk and expect breaking changes.
>
> **Installation instructions below may become outdated quickly as the project evolves.**

A Model Context Protocol (MCP) server that provides Kotlin language server diagnostics capabilities.

## Overview

This project implements an MCP server that integrates with the Kotlin Language Server Protocol (LSP) to provide
diagnostic information for Kotlin code. It allows AI assistants and other tools to analyze Kotlin projects and retrieve
compilation errors, warnings, and other diagnostic information.

## Features

- **LSP Integration**: Connects to Kotlin Language Server for real-time diagnostics
- **MCP Protocol**: Exposes diagnostics through the Model Context Protocol
- **File Analysis**: Analyze individual Kotlin files or entire projects
- **Comprehensive Logging**: Detailed logging for debugging and monitoring

## Prerequisites

### For Homebrew Installation

- None! All dependencies are automatically installed

### For Manual Installation
- **Java 21** or higher
- **Kotlin LSP Server**

## Installation

### Option 1: Homebrew (Recommended)

The easiest way to install is by tapping the GitHub repository:

```bash
# Add the tap from GitHub
brew tap dmitchelljackson/kotlin-diagnostics-mcp https://github.com/dmitchelljackson/Kotlin-Diagnostics-Mcp.git

# Install the package
brew install kotlin-diagnostics-mcp
```

Or install directly from the formula file in one command:

```bash
# Install directly from the GitHub repository
brew install dmitchelljackson/kotlin-diagnostics-mcp/kotlin-diagnostics-mcp
```

This will automatically:

- Install OpenJDK 21
- Install Kotlin LSP server
- Set up the `kotlin-diagnostics-mcp` command globally

### Option 2: Manual Installation

#### Prerequisites Setup

1. **Install Java 21**:
   ```bash
   # macOS (Homebrew)
   brew install openjdk@21
   
   # Ubuntu/Debian
   sudo apt update
   sudo apt install openjdk-21-jdk
   
   # Windows
   # Download from https://adoptium.net/
   ```

2. **Install Kotlin LSP Server**:
   ```bash
   # Via Homebrew
   brew tap jetbrains/utils
   brew install kotlin-lsp
   
   # Or download manually from JetBrains releases
   ```

#### Build from Source

1. **Clone the repository**:
   ```bash
   git clone https://github.com/dmitchelljackson/Kotlin-Diagnostics-Mcp.git
   cd Kotlin-Diagnostics-Mcp
   ```

2. **Build the project**:
   ```bash
   ./gradlew build
   ```

3. **Create the executable JAR**:
   ```bash
   ./gradlew shadowJar
   ```

   This creates `build/libs/kotlin-diagnostics.jar`

4. **Run the application**:
   ```bash
   java -jar build/libs/kotlin-diagnostics.jar
   ```

## Usage

### Running the MCP Server

After installation, start the MCP server:

```bash
# If installed via Homebrew
kotlin-diagnostics-mcp

# If built from source
java -jar kotlin-diagnostics.jar
```

### Development and Testing

#### Run Diagnostics Test

Test the diagnostic capabilities on sample Kotlin files:

```bash
./gradlew testDiagnostics
```

This will:

- Start the Kotlin Language Server
- Analyze test files in the `test_files/` directory
- Display diagnostic results
- Log detailed information to `logs/kotlin-diagnostics-mcp.log`

## Configuration

### Logging

The application uses SLF4J for logging. Log files are written to the `logs/` directory:

- **Log Level**: Configurable via system properties
- **Log File**: `logs/kotlin-diagnostics-mcp.log`
- **Console Output**: INFO level and above

To adjust logging levels:

```bash
java -Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG -jar kotlin-diagnostics.jar
```

## Related Projects

- [Kotlin Language Server](https://github.com/fwcd/kotlin-language-server)
- [LSP4J](https://github.com/eclipse/lsp4j)
- [Model Context Protocol](https://modelcontextprotocol.io/)

## Support

- **Issues**: [GitHub Issues](https://github.com/dmitchelljackson/Kotlin-Diagnostics-Mcp/issues)
- **Discussions**: [GitHub Discussions](https://github.com/dmitchelljackson/Kotlin-Diagnostics-Mcp/discussions)