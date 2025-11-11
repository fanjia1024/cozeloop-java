package com.coze.loop.config;

import com.coze.loop.http.HttpConfig;
import com.coze.loop.prompt.PromptCache;
import com.coze.loop.trace.CozeLoopTracerProvider;

/**
 * Main configuration for CozeLoop Client.
 */
public class CozeLoopConfig {
    private String workspaceId;
    private String serviceName = "cozeloop-java-app";
    private String baseUrl = "https://loop.coze.cn";
    
    private HttpConfig httpConfig;
    private CozeLoopTracerProvider.TraceConfig traceConfig;
    private PromptCache.PromptCacheConfig promptCacheConfig;
    
    public CozeLoopConfig() {
        this.httpConfig = HttpConfig.builder().build();
        this.traceConfig = CozeLoopTracerProvider.TraceConfig.builder().build();
        this.promptCacheConfig = PromptCache.PromptCacheConfig.builder().build();
    }
    
    // Getters and Setters
    public String getWorkspaceId() {
        return workspaceId;
    }
    
    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
    
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    public HttpConfig getHttpConfig() {
        return httpConfig;
    }
    
    public void setHttpConfig(HttpConfig httpConfig) {
        this.httpConfig = httpConfig;
    }
    
    public CozeLoopTracerProvider.TraceConfig getTraceConfig() {
        return traceConfig;
    }
    
    public void setTraceConfig(CozeLoopTracerProvider.TraceConfig traceConfig) {
        this.traceConfig = traceConfig;
    }
    
    public PromptCache.PromptCacheConfig getPromptCacheConfig() {
        return promptCacheConfig;
    }
    
    public void setPromptCacheConfig(PromptCache.PromptCacheConfig promptCacheConfig) {
        this.promptCacheConfig = promptCacheConfig;
    }
    
    /**
     * Get span endpoint URL.
     */
    public String getSpanEndpoint() {
        return baseUrl + "/api/v1/spans";
    }
    
    /**
     * Get file endpoint URL.
     */
    public String getFileEndpoint() {
        return baseUrl + "/api/v1/files";
    }
    
    /**
     * Get prompt endpoint URL.
     */
    public String getPromptEndpoint() {
        return baseUrl + "/api/v1/prompts";
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private final CozeLoopConfig config = new CozeLoopConfig();
        
        public Builder workspaceId(String workspaceId) {
            config.workspaceId = workspaceId;
            return this;
        }
        
        public Builder serviceName(String serviceName) {
            config.serviceName = serviceName;
            return this;
        }
        
        public Builder baseUrl(String baseUrl) {
            config.baseUrl = baseUrl;
            return this;
        }
        
        public Builder httpConfig(HttpConfig httpConfig) {
            config.httpConfig = httpConfig;
            return this;
        }
        
        public Builder traceConfig(CozeLoopTracerProvider.TraceConfig traceConfig) {
            config.traceConfig = traceConfig;
            return this;
        }
        
        public Builder promptCacheConfig(PromptCache.PromptCacheConfig cacheConfig) {
            config.promptCacheConfig = cacheConfig;
            return this;
        }
        
        public CozeLoopConfig build() {
            return config;
        }
    }
}

