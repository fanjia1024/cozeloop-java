package com.coze.loop.trace;

import com.coze.loop.entity.UploadFile;
import com.coze.loop.entity.UploadSpan;
import com.coze.loop.exception.ExportException;
import com.coze.loop.exception.ErrorCode;
import com.coze.loop.http.HttpClient;
import com.coze.loop.internal.JsonUtils;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom SpanExporter that exports spans to CozeLoop platform.
 */
public class CozeLoopSpanExporter implements SpanExporter {
    private static final Logger logger = LoggerFactory.getLogger(CozeLoopSpanExporter.class);
    
    private final HttpClient httpClient;
    private final String spanEndpoint;
    private final String fileEndpoint;
    private final String workspaceId;
    private final String serviceName;
    private final FileUploader fileUploader;
    
    private volatile boolean isShutdown = false;
    
    public CozeLoopSpanExporter(HttpClient httpClient,
                                String spanEndpoint,
                                String fileEndpoint,
                                String workspaceId,
                                String serviceName) {
        this.httpClient = httpClient;
        this.spanEndpoint = spanEndpoint;
        this.fileEndpoint = fileEndpoint;
        this.workspaceId = workspaceId;
        this.serviceName = serviceName;
        this.fileUploader = new FileUploader(httpClient, fileEndpoint, workspaceId);
    }
    
    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        if (isShutdown) {
            return CompletableResultCode.ofFailure();
        }
        
        try {
            List<UploadSpan> uploadSpans = new ArrayList<>();
            
            for (SpanData spanData : spans) {
                // Convert span
                UploadSpan uploadSpan = SpanConverter.convert(spanData, workspaceId, serviceName);
                
                // Extract and upload files if needed
                List<UploadFile> files = fileUploader.extractFiles(spanData);
                if (!files.isEmpty()) {
                    String objectStorage = fileUploader.uploadFiles(files);
                    if (objectStorage != null) {
                        uploadSpan.setObjectStorage(objectStorage);
                    }
                }
                
                uploadSpans.add(uploadSpan);
            }
            
            // Batch upload spans
            if (!uploadSpans.isEmpty()) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("spans", uploadSpans);
                
                httpClient.post(spanEndpoint, payload);
                logger.debug("Exported {} spans to CozeLoop", uploadSpans.size());
            }
            
            return CompletableResultCode.ofSuccess();
        } catch (Exception e) {
            logger.error("Failed to export spans", e);
            return CompletableResultCode.ofFailure();
        }
    }
    
    @Override
    public CompletableResultCode flush() {
        // No buffering in this exporter, so flush is a no-op
        return CompletableResultCode.ofSuccess();
    }
    
    @Override
    public CompletableResultCode shutdown() {
        if (isShutdown) {
            return CompletableResultCode.ofSuccess();
        }
        
        isShutdown = true;
        
        try {
            httpClient.close();
            return CompletableResultCode.ofSuccess();
        } catch (Exception e) {
            logger.error("Failed to shutdown exporter", e);
            return CompletableResultCode.ofFailure();
        }
    }
}

