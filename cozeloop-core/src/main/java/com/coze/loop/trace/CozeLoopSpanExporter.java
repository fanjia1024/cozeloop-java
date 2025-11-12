package com.coze.loop.trace;

import com.coze.loop.entity.UploadFile;
import com.coze.loop.entity.UploadSpan;
import com.coze.loop.http.HttpClient;
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
 * Spans are exported in batches of 25 to the remote server.
 */
public class CozeLoopSpanExporter implements SpanExporter {
    private static final Logger logger = LoggerFactory.getLogger(CozeLoopSpanExporter.class);
    
    /**
     * Batch size for exporting spans to remote server.
     * Each batch contains at most 25 spans.
     */
    private static final int EXPORT_BATCH_SIZE = 25;
    
    private final HttpClient httpClient;
    private final String spanEndpoint;
    @SuppressWarnings("unused") // Used by FileUploader constructor
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
    public CompletableResultCode export(@javax.annotation.Nonnull Collection<SpanData> spans) {
        if (isShutdown) {
            return CompletableResultCode.ofFailure();
        }
        
        if (spans == null || spans.isEmpty()) {
            return CompletableResultCode.ofSuccess();
        }
        
        // Convert all spans to upload spans first
        List<UploadSpan> uploadSpans = new ArrayList<>();
        try {
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
        } catch (Exception e) {
            logger.error("Failed to convert spans for export", e);
            return CompletableResultCode.ofFailure();
        }
        
        if (uploadSpans.isEmpty()) {
            return CompletableResultCode.ofSuccess();
        }
        
        // Export spans in batches of EXPORT_BATCH_SIZE
        int totalSpans = uploadSpans.size();
        int totalBatches = (totalSpans + EXPORT_BATCH_SIZE - 1) / EXPORT_BATCH_SIZE;
        int successCount = 0;
        int failureCount = 0;
        
        logger.debug("Exporting {} spans in {} batches (batch size: {})", 
            totalSpans, totalBatches, EXPORT_BATCH_SIZE);
        
        for (int i = 0; i < totalBatches; i++) {
            int start = i * EXPORT_BATCH_SIZE;
            int end = Math.min(start + EXPORT_BATCH_SIZE, totalSpans);
            List<UploadSpan> batch = uploadSpans.subList(start, end);
            
            try {
                // Export this batch
                exportBatch(batch, i + 1, totalBatches);
                successCount++;
                logger.debug("Successfully exported batch {}/{} ({} spans)", 
                    i + 1, totalBatches, batch.size());
            } catch (Exception e) {
                failureCount++;
                logger.error("Failed to export batch {}/{} ({} spans): {}", 
                    i + 1, totalBatches, batch.size(), e.getMessage(), e);
                // Continue processing other batches even if one fails
            }
        }
        
        // Log summary
        if (failureCount == 0) {
            logger.debug("Successfully exported all {} spans in {} batches", 
                totalSpans, totalBatches);
            return CompletableResultCode.ofSuccess();
        } else {
            logger.warn("Exported {} spans: {} batches succeeded, {} batches failed", 
                totalSpans, successCount, failureCount);
            // Return failure if any batch failed
            return CompletableResultCode.ofFailure();
        }
    }
    
    /**
     * Export a single batch of spans to the remote server.
     *
     * @param batch the batch of upload spans to export
     * @param batchNumber the batch number (1-based)
     * @param totalBatches the total number of batches
     * @throws Exception if the export fails
     */
    private void exportBatch(List<UploadSpan> batch, int batchNumber, int totalBatches) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("spans", batch);
        
        httpClient.post(spanEndpoint, payload);
        
        if (logger.isTraceEnabled()) {
            logger.trace("Exported batch {}/{} with {} spans to CozeLoop", 
                batchNumber, totalBatches, batch.size());
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

