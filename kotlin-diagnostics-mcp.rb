class KotlinDiagnosticsMcp < Formula
  desc "Model Context Protocol server for Kotlin LSP diagnostics"
  homepage "https://github.com/YOUR_USERNAME/Kotlin-Diagnostics-Mcp"
  url "https://github.com/YOUR_USERNAME/Kotlin-Diagnostics-Mcp/releases/download/v1.0.0/kotlin-diagnostics.jar"
  version "1.0.0"

  # Dependencies that will be automatically installed
  depends_on "openjdk@21"

  # Add the JetBrains tap and install kotlin-lsp
  def self.add_jetbrains_tap
    system "brew", "tap", "jetbrains/utils" unless system("brew tap | grep -q jetbrains/utils")
  end

  def install
    # Ensure JetBrains tap is added
    self.class.add_jetbrains_tap
    
    # Install kotlin-lsp if not already installed
    unless system("brew list kotlin-lsp > /dev/null 2>&1")
      system "brew", "install", "kotlin-lsp"
    end
    
    # Install the JAR file
    libexec.install "kotlin-diagnostics.jar"
    
    # Create wrapper script that works on both macOS and Linux
    (bin/"kotlin-diagnostics-mcp").write <<~EOS
      #!/bin/bash
      
      # Cross-platform Homebrew Java detection
      # This works on both macOS and Linux (Linuxbrew)
      HOMEBREW_JAVA="$(brew --prefix openjdk@21)/bin/java"
      
      # Fallback check in case brew --prefix fails
      if [ ! -x "$HOMEBREW_JAVA" ]; then
        # Try common Homebrew paths
        for brew_path in /opt/homebrew /usr/local /home/linuxbrew/.linuxbrew; do
          if [ -x "$brew_path/opt/openjdk@21/bin/java" ]; then
            HOMEBREW_JAVA="$brew_path/opt/openjdk@21/bin/java"
            break
          fi
        done
      fi
      
      # Final check that we found a working Java
      if [ ! -x "$HOMEBREW_JAVA" ]; then
        echo "Error: Could not find Homebrew OpenJDK 21 installation"
        echo "Please ensure OpenJDK 21 is installed via Homebrew: brew install openjdk@21"
        exit 1
      fi
      
      # Run the JAR with the specific Java version, passing all arguments through
      exec "$HOMEBREW_JAVA" -jar "#{libexec}/kotlin-diagnostics.jar" "$@"
    EOS
    
    # Make the script executable
    chmod 0755, bin/"kotlin-diagnostics-mcp"
  end

  def post_install
    # Verify installation
    ohai "Verifying installation..."
    
    # Check if dependencies are properly installed
    unless system("brew list openjdk@21 > /dev/null 2>&1")
      opoo "OpenJDK 21 may not be properly installed"
    end
    
    unless system("brew list kotlin-lsp > /dev/null 2>&1")
      opoo "Kotlin LSP may not be properly installed"
    end
    
    # Provide usage instructions
    ohai "Installation complete!"
    puts ""
    puts "You can now use 'kotlin-diagnostics-mcp' globally:"
    puts "  kotlin-diagnostics-mcp"
    puts ""
    puts "If the command is not found, ensure Homebrew's bin is in your PATH:"
    if OS.mac?
      puts "  echo 'export PATH=\"$(brew --prefix)/bin:$PATH\"' >> ~/.zshrc"
      puts "  source ~/.zshrc"
    else
      puts "  echo 'eval \"$(/home/linuxbrew/.linuxbrew/bin/brew shellenv)\"' >> ~/.bashrc"
      puts "  source ~/.bashrc"
    end
  end

  test do
    # Test that the binary exists and is executable
    assert_predicate bin/"kotlin-diagnostics-mcp", :exist?
    assert_predicate bin/"kotlin-diagnostics-mcp", :executable?
    
    # Test that Java is accessible (cross-platform)
    java_path = `brew --prefix openjdk@21`.strip + "/bin/java"
    assert_predicate Pathname.new(java_path), :exist?
    
    # Test that the JAR file exists
    assert_predicate libexec/"kotlin-diagnostics.jar", :exist?
    
    # Test basic invocation (capture output to avoid hanging)
    # Use a timeout to prevent hanging tests
    system "timeout", "10s", bin/"kotlin-diagnostics-mcp", "--help" if which("timeout")
  end
  
  def caveats
    platform_info = OS.mac? ? "macOS" : "Linux"
    
    <<~EOS
      kotlin-diagnostics-mcp has been installed successfully on #{platform_info}!
      
      The tool is now available globally as 'kotlin-diagnostics-mcp'.
      
      Dependencies installed:
      • OpenJDK 21 (#{Formula["openjdk@21"].installed? ? "✓" : "✗"})
      • Kotlin LSP (#{system("brew list kotlin-lsp > /dev/null 2>&1") ? "✓" : "✗"})
      
      Platform-specific setup:
      #{OS.mac? ? 
        "macOS: Add to ~/.zshrc: export PATH=\"$(brew --prefix)/bin:$PATH\"" : 
        "Linux: Add to ~/.bashrc: eval \"$(/home/linuxbrew/.linuxbrew/bin/brew shellenv)\""
      }
    EOS
  end
end