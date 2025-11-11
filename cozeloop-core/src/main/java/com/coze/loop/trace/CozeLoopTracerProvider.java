package com.coze.loop.trace;

import com.coze.loop.http.HttpClient;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.ResourceAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * CozeLoop TracerProvider that wraps OpenTelemetry TracerProvider.
 */
public class CozeLoopTracerProvider {
    private static final Logger logger = LoggerFactory.getLogger(CozeLoopTracerProvider.class);
    
    private final SdkTracerProvider sdkTracerProvider;
    private final OpenTelemetrySdk openTelemetrySdk;
    private final CozeLoopSpanExporter spanExporter;
    
    private CozeLoopTracerProvider(HttpClient httpClient,
                                    String spanEndpoint,
                                    String fileEndpoint,
                                    String workspaceId,
                                    String serviceName,
                                    TraceConfig config) {
        // Create span exporter
        this.spanExporter = new CozeLoopSpanExporter(
            httpClient, spanEndpoint, fileEndpoint, workspaceId, serviceName);
        
        // Create resource
        Resource resource = Resource.getDefault()
            .merge(Resource.builder()
                .put(ResourceAttributes.SERVICE_NAME, serviceName)
                .put("workspace.id", workspaceId)
                .build());
        
        // Create batch span processor
        BatchSpanProcessor batchProcessor = BatchSpanProcessor.builder(spanExporter)
            .setMaxQueueSize(config.getMaxQueueSize())
            .setMaxExportBatchSize(config.getBatchSize())
            .setScheduleDelay(config.getScheduleDelayMillis(), TimeUnit.MILLISECONDS)
            .setExporterTimeout(config.getExportTimeoutMillis(), TimeUnit.MILLISECONDS)
            .build();
        
        // Create tracer provider
        this.sdkTracerProvider = SdkTracerProvider.builder()
            .setResource(resource)
            .addSpanProcessor(batchProcessor)
            .build();
        
        // Build and register OpenTelemetry SDK
        this.openTelemetrySdk = OpenTelemetrySdk.builder()
            .setTracerProvider(sdkTracerProvider)
            .buildAndRegisterGlobal();
        
        logger.info("CozeLoop TracerProvider initialized with service: {}, workspace: {}",
            serviceName, workspaceId);
    }
    
    /**
     * Create a new CozeLoopTracerProvider.
     *
     * @param httpClient the HTTP client
     * @param spanEndpoint the span upload endpoint
     * @param fileEndpoint the file upload endpoint
     * @param workspaceId the workspace ID
     * @param serviceName the service name
     * @param config the trace configuration
     * @return CozeLoopTracerProvider instance
     */
    public static CozeLoopTracerProvider create(HttpClient httpClient,
                                                 String spanEndpoint,
                                                 String fileEndpoint,
                                                 String workspaceId,
                                                 String serviceName,
                                                 TraceConfig config) {
        return new CozeLoopTracerProvider(
            httpClient, spanEndpoint, fileEndpoint, workspaceId, serviceName, config);
    }
    
    /**
     * Get a tracer.
     *
     * @param instrumentationName the instrumentation name
     * @return Tracer instance
     */
    public Tracer getTracer(String instrumentationName) {
        return openTelemetrySdk.getTracer(instrumentationName);
    }
    
    /**
     * Get the TracerProvider.
     *
     * @return TracerProvider instance
     */
    public TracerProvider getTracerProvider() {
        return sdkTracerProvider;
    }
    
    /**
     * Shutdown the tracer provider and flush all pending spans.
     */
    public void shutdown() {
        logger.info("Shutting down CozeLoop TracerProvider");
        try {
            sdkTracerProvider.forceFlush().join(10, TimeUnit.SECONDS);
            sdkTracerProvider.shutdown().join(10, TimeUnit.SECONDS);
            spanExporter.shutdown();
        } catch (Exception e) {
            logger.error("Error shutting down tracer provider", e);
        }
    }
    
    /**
     * Trace configuration.
     */
    public static class TraceConfig {
        private int maxQueueSize = 2048;
        private int batchSize = 512;
        private long scheduleDelayMillis = 5000;
        private long exportTimeoutMillis = 30000;
        
        public int getMaxQueueSize() {
            return maxQueueSize;
        }
        
        public void setMaxQueueSize(int maxQueueSize) {
            this.maxQueueSize = maxQueueSize;
        }
        
        public int getBatchSize() {
            return batchSize;
        }
        
        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }
        
        public long getScheduleDelayMillis() {
            return scheduleDelayMillis;
        }
        
        public void setScheduleDelayMillis(long scheduleDelayMillis) {
            this.scheduleDelayMillis = scheduleDelayMillis;
        }
        
        public long getExportTimeoutMillis() {
            return exportTimeoutMillis;
        }
        
        public void setExportTimeoutMillis(long exportTimeoutMillis) {
            this.exportTimeoutMillis = exportTimeoutMillis;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private final TraceConfig config = new TraceConfig();
            
            public Builder maxQueueSize(int size) {
                config.maxQueueSize = size;
                return this;
            }
            
            public Builder batchSize(int size) {
                config.batchSize = size;
                return this;
            }
            
            public Builder scheduleDelayMillis(long millis) {
                config.scheduleDelayMillis = millis;
                return this;
            }
            
            public Builder exportTimeoutMillis(long millis) {
                config.exportTimeoutMillis = millis;
                return this;
            }
            
            public TraceConfig build() {
                return config;
            }
        }
    }
}

