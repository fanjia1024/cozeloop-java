# CozeLoop Java SDK

Java SDK for interacting with [CozeLoop platform](https://loop.coze.cn).

## Features

- **Trace Reporting**: Built on OpenTelemetry SDK, automatic batch reporting
- **Prompt Management**: Pull, cache, and format prompts
- **AOP Annotation**: Declarative tracing with `@CozeTrace` annotation
- **Spring Boot Integration**: Seamless integration with Spring Boot applications

## Requirements

- Java 8+ (Java 11+ recommended)
- Maven 3.6+ or Gradle 6.0+

## Installation

### Maven

```xml
<dependency>
    <groupId>com.coze.loop</groupId>
    <artifactId>cozeloop-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Spring Boot Starter

```xml
<dependency>
    <groupId>com.coze.loop</groupId>
    <artifactId>cozeloop-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick Start

### Basic Usage

```java
// Initialize client
CozeLoopClient client = CozeLoopClient.builder()
    .workspaceId("your_workspace_id")
    .jwtOAuth("client_id", "private_key", "public_key_id")
    .build();

// Report trace
try (CozeLoopSpan span = client.startSpan("operation", "custom")) {
    span.setInput("Hello");
    span.setOutput("World");
}

// Get and format prompt
Prompt prompt = client.getPrompt(GetPromptParam.builder()
    .promptKey("your_prompt_key")
    .build());
    
List<Message> messages = client.formatPrompt(prompt, 
    Map.of("var1", "content"));

// Close client
client.close();
```

### Spring Boot Integration

**application.yml**

```yaml
cozeloop:
  workspace-id: your_workspace_id
  jwt:
    client-id: your_client_id
    private-key: your_private_key
    public-key-id: your_public_key_id
  trace:
    enabled: true
```

**Use Annotations**

```java
@Service
public class LLMService {
    
    @CozeTrace(value = "llm_call", spanType = "llm", captureArgs = true, captureReturn = true)
    public String callLLM(String prompt) {
        return llmClient.call(prompt);
    }
}
```

## Documentation

- [API Documentation](docs/api.md)
- [Examples](docs/examples.md)
- [Configuration](docs/configuration.md)

## Building from Source

```bash
git clone https://github.com/coze-dev/cozeloop-java.git
cd cozeloop-java
mvn clean install
```

## License

This project is licensed under the [MIT License](LICENSE).

