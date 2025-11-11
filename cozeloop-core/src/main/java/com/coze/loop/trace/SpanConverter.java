package com.coze.loop.trace;

import com.coze.loop.entity.UploadSpan;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.trace.data.SpanData;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Converter to transform OpenTelemetry SpanData to CozeLoop UploadSpan format.
 */
public final class SpanConverter {
    
    private SpanConverter() {
        // Utility class
    }
    
    /**
     * Convert OpenTelemetry SpanData to CozeLoop UploadSpan.
     *
     * @param spanData the OpenTelemetry span data
     * @param workspaceId the workspace ID
     * @param serviceName the service name
     * @return converted UploadSpan
     */
    public static UploadSpan convert(SpanData spanData, String workspaceId, String serviceName) {
        UploadSpan uploadSpan = new UploadSpan();
        
        // Basic span info
        uploadSpan.setTraceId(spanData.getTraceId());
        uploadSpan.setSpanId(spanData.getSpanId());
        uploadSpan.setParentId(spanData.getParentSpanId());
        uploadSpan.setLogId(spanData.getSpanId()); // Use span ID as log ID
        uploadSpan.setWorkspaceId(workspaceId);
        uploadSpan.setServiceName(serviceName);
        
        // Timing info (convert from nanoseconds to microseconds)
        long startMicros = TimeUnit.NANOSECONDS.toMicros(spanData.getStartEpochNanos());
        long endMicros = TimeUnit.NANOSECONDS.toMicros(spanData.getEndEpochNanos());
        uploadSpan.setStartedAtMicros(startMicros);
        uploadSpan.setDurationMicros(endMicros - startMicros);
        
        // Span name and type
        uploadSpan.setSpanName(spanData.getName());
        
        // Extract attributes
        Attributes attributes = spanData.getAttributes();
        
        // Span type
        String spanType = attributes.get(AttributeKey.stringKey("span.type"));
        uploadSpan.setSpanType(spanType != null ? spanType : "custom");
        
        // Status code
        int statusCode = convertStatusCode(spanData.getStatus().getStatusCode());
        uploadSpan.setStatusCode(statusCode);
        
        // Input and output
        String input = attributes.get(AttributeKey.stringKey("cozeloop.input"));
        String output = attributes.get(AttributeKey.stringKey("cozeloop.output"));
        uploadSpan.setInput(input);
        uploadSpan.setOutput(output);
        
        // Object storage key (for multimodality)
        String objectStorage = attributes.get(AttributeKey.stringKey("cozeloop.object_storage"));
        uploadSpan.setObjectStorage(objectStorage);
        
        // Extract tags by type
        Map<String, String> tagsString = new HashMap<>();
        Map<String, Long> tagsLong = new HashMap<>();
        Map<String, Double> tagsDouble = new HashMap<>();
        Map<String, Boolean> tagsBool = new HashMap<>();
        Map<String, String> systemTagsString = new HashMap<>();
        Map<String, Long> systemTagsLong = new HashMap<>();
        Map<String, Double> systemTagsDouble = new HashMap<>();
        
        attributes.forEach((key, value) -> {
            String keyStr = key.getKey();
            
            // Skip special cozeloop attributes
            if (keyStr.startsWith("cozeloop.")) {
                return;
            }
            
            boolean isSystemTag = keyStr.startsWith("system.");
            
            if (value instanceof String) {
                if (isSystemTag) {
                    systemTagsString.put(keyStr, (String) value);
                } else {
                    tagsString.put(keyStr, (String) value);
                }
            } else if (value instanceof Long) {
                if (isSystemTag) {
                    systemTagsLong.put(keyStr, (Long) value);
                } else {
                    tagsLong.put(keyStr, (Long) value);
                }
            } else if (value instanceof Double) {
                if (isSystemTag) {
                    systemTagsDouble.put(keyStr, (Double) value);
                } else {
                    tagsDouble.put(keyStr, (Double) value);
                }
            } else if (value instanceof Boolean) {
                if (!isSystemTag) {
                    tagsBool.put(keyStr, (Boolean) value);
                }
            } else if (value instanceof Integer) {
                long longValue = ((Integer) value).longValue();
                if (isSystemTag) {
                    systemTagsLong.put(keyStr, longValue);
                } else {
                    tagsLong.put(keyStr, longValue);
                }
            }
        });
        
        if (!tagsString.isEmpty()) {
            uploadSpan.setTagsString(tagsString);
        }
        if (!tagsLong.isEmpty()) {
            uploadSpan.setTagsLong(tagsLong);
        }
        if (!tagsDouble.isEmpty()) {
            uploadSpan.setTagsDouble(tagsDouble);
        }
        if (!tagsBool.isEmpty()) {
            uploadSpan.setTagsBool(tagsBool);
        }
        if (!systemTagsString.isEmpty()) {
            uploadSpan.setSystemTagsString(systemTagsString);
        }
        if (!systemTagsLong.isEmpty()) {
            uploadSpan.setSystemTagsLong(systemTagsLong);
        }
        if (!systemTagsDouble.isEmpty()) {
            uploadSpan.setSystemTagsDouble(systemTagsDouble);
        }
        
        return uploadSpan;
    }
    
    /**
     * Convert OpenTelemetry StatusCode to CozeLoop status code.
     */
    private static int convertStatusCode(StatusCode statusCode) {
        switch (statusCode) {
            case OK:
                return 0;
            case ERROR:
                return 1;
            default:
                return 2; // UNSET
        }
    }
}

