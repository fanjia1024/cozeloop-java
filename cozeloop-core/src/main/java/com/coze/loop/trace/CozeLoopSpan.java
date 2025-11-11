package com.coze.loop.trace;

import com.coze.loop.internal.JsonUtils;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

/**
 * Wrapper for OpenTelemetry Span that provides CozeLoop-specific methods.
 */
public class CozeLoopSpan implements AutoCloseable {
    private final Span span;
    private final Scope scope;
    
    public CozeLoopSpan(Span span, Scope scope) {
        this.span = span;
        this.scope = scope;
    }
    
    /**
     * Set the input for this span.
     *
     * @param input the input object
     * @return this span
     */
    public CozeLoopSpan setInput(Object input) {
        if (input != null) {
            String inputStr = input instanceof String ? 
                (String) input : JsonUtils.toJson(input);
            span.setAttribute(AttributeKey.stringKey("cozeloop.input"), inputStr);
        }
        return this;
    }
    
    /**
     * Set the output for this span.
     *
     * @param output the output object
     * @return this span
     */
    public CozeLoopSpan setOutput(Object output) {
        if (output != null) {
            String outputStr = output instanceof String ? 
                (String) output : JsonUtils.toJson(output);
            span.setAttribute(AttributeKey.stringKey("cozeloop.output"), outputStr);
        }
        return this;
    }
    
    /**
     * Set the error for this span.
     *
     * @param error the error/exception
     * @return this span
     */
    public CozeLoopSpan setError(Throwable error) {
        if (error != null) {
            span.setStatus(StatusCode.ERROR, error.getMessage());
            span.recordException(error);
        }
        return this;
    }
    
    /**
     * Set status code (0=OK, 1=ERROR).
     *
     * @param statusCode the status code
     * @return this span
     */
    public CozeLoopSpan setStatusCode(int statusCode) {
        if (statusCode == 0) {
            span.setStatus(StatusCode.OK);
        } else {
            span.setStatus(StatusCode.ERROR);
        }
        return this;
    }
    
    /**
     * Set model provider (e.g., "openai", "anthropic").
     *
     * @param provider the model provider
     * @return this span
     */
    public CozeLoopSpan setModelProvider(String provider) {
        if (provider != null) {
            span.setAttribute(AttributeKey.stringKey("llm.provider"), provider);
        }
        return this;
    }
    
    /**
     * Set model name.
     *
     * @param model the model name
     * @return this span
     */
    public CozeLoopSpan setModel(String model) {
        if (model != null) {
            span.setAttribute(AttributeKey.stringKey("llm.model"), model);
        }
        return this;
    }
    
    /**
     * Set input tokens.
     *
     * @param tokens the number of input tokens
     * @return this span
     */
    public CozeLoopSpan setInputTokens(long tokens) {
        span.setAttribute(AttributeKey.longKey("llm.input_tokens"), tokens);
        return this;
    }
    
    /**
     * Set output tokens.
     *
     * @param tokens the number of output tokens
     * @return this span
     */
    public CozeLoopSpan setOutputTokens(long tokens) {
        span.setAttribute(AttributeKey.longKey("llm.output_tokens"), tokens);
        return this;
    }
    
    /**
     * Set total tokens.
     *
     * @param tokens the total number of tokens
     * @return this span
     */
    public CozeLoopSpan setTotalTokens(long tokens) {
        span.setAttribute(AttributeKey.longKey("llm.total_tokens"), tokens);
        return this;
    }
    
    /**
     * Set custom attribute (string).
     *
     * @param key the attribute key
     * @param value the attribute value
     * @return this span
     */
    public CozeLoopSpan setAttribute(String key, String value) {
        if (key != null && value != null) {
            span.setAttribute(AttributeKey.stringKey(key), value);
        }
        return this;
    }
    
    /**
     * Set custom attribute (long).
     *
     * @param key the attribute key
     * @param value the attribute value
     * @return this span
     */
    public CozeLoopSpan setAttribute(String key, long value) {
        if (key != null) {
            span.setAttribute(AttributeKey.longKey(key), value);
        }
        return this;
    }
    
    /**
     * Set custom attribute (double).
     *
     * @param key the attribute key
     * @param value the attribute value
     * @return this span
     */
    public CozeLoopSpan setAttribute(String key, double value) {
        if (key != null) {
            span.setAttribute(AttributeKey.doubleKey(key), value);
        }
        return this;
    }
    
    /**
     * Set custom attribute (boolean).
     *
     * @param key the attribute key
     * @param value the attribute value
     * @return this span
     */
    public CozeLoopSpan setAttribute(String key, boolean value) {
        if (key != null) {
            span.setAttribute(AttributeKey.booleanKey(key), value);
        }
        return this;
    }
    
    /**
     * Get the underlying OpenTelemetry Span.
     *
     * @return the underlying span
     */
    public Span getSpan() {
        return span;
    }
    
    /**
     * End the span and close the scope.
     */
    @Override
    public void close() {
        try {
            span.end();
        } finally {
            scope.close();
        }
    }
}

