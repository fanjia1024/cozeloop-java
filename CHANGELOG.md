# Changelog


## [1.0.0-SNAPSHOT] - 2025-11-11

### Added

- Initial CozeLoop Java SDK release
- Core SDK module (`cozeloop-core`) with trace reporting and prompt management
- Spring Boot starter module (`cozeloop-spring-boot-starter`) for seamless integration
- Trace reporting functionality built on OpenTelemetry SDK
- Prompt management with caching and formatting support
- Support for Jinja template engine in prompts
- AOP annotation `@CozeTrace` for declarative tracing
- OAuth JWT and PAT (Personal Access Token) authentication
- Example code demonstrating SDK usage
- Comprehensive documentation and README files
- Maven multi-module project structure
- Checkstyle configuration for code quality

### Features

- **Trace Reporting**: Automatic batch reporting of traces to CozeLoop platform
- **Prompt Management**: Pull, cache, and format prompts from the platform
- **Spring Boot Integration**: Auto-configuration and AOP support
- **Thread-Safe**: All core components are thread-safe
- **Java 8+ Compatible**: Wide compatibility with Java 8 and above

