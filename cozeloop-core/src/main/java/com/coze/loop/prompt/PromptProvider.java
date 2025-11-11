package com.coze.loop.prompt;

import com.coze.loop.entity.Message;
import com.coze.loop.entity.Prompt;
import com.coze.loop.exception.ErrorCode;
import com.coze.loop.exception.PromptException;
import com.coze.loop.http.HttpClient;
import com.coze.loop.internal.JsonUtils;
import com.coze.loop.internal.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provider for prompt operations: fetch, cache, and format.
 */
public class PromptProvider {
    private static final Logger logger = LoggerFactory.getLogger(PromptProvider.class);
    
    private final HttpClient httpClient;
    private final String promptEndpoint;
    private final String workspaceId;
    private final PromptCache cache;
    private final PromptFormatter formatter;
    
    public PromptProvider(HttpClient httpClient,
                         String promptEndpoint,
                         String workspaceId,
                         PromptCache.PromptCacheConfig cacheConfig) {
        this.httpClient = httpClient;
        this.promptEndpoint = promptEndpoint;
        this.workspaceId = workspaceId;
        this.formatter = new PromptFormatter();
        
        // Initialize cache with this provider as the loader
        this.cache = new PromptCache(cacheConfig, this::fetchPromptFromServer);
    }
    
    /**
     * Get a prompt (with caching).
     *
     * @param param the parameters for getting prompt
     * @return prompt
     */
    public Prompt getPrompt(GetPromptParam param) {
        ValidationUtils.requireNonEmpty(param.getPromptKey(), "promptKey");
        
        String cacheKey = buildCacheKey(param);
        
        try {
            return cache.getSync(cacheKey);
        } catch (Exception e) {
            throw new PromptException(ErrorCode.PROMPT_NOT_FOUND,
                "Failed to get prompt: " + param.getPromptKey(), e);
        }
    }
    
    /**
     * Format prompt with variables.
     *
     * @param prompt the prompt
     * @param variables the variables to substitute
     * @return formatted messages
     */
    public List<Message> formatPrompt(Prompt prompt, Map<String, Object> variables) {
        ValidationUtils.requireNonNull(prompt, "prompt");
        
        if (variables == null) {
            variables = new HashMap<>();
        }
        
        return formatter.format(prompt, variables);
    }
    
    /**
     * Get and format prompt in one call.
     *
     * @param param the parameters for getting prompt
     * @param variables the variables to substitute
     * @return formatted messages
     */
    public List<Message> getAndFormat(GetPromptParam param, Map<String, Object> variables) {
        Prompt prompt = getPrompt(param);
        return formatPrompt(prompt, variables);
    }
    
    /**
     * Invalidate cache for a prompt.
     *
     * @param param the parameters identifying the prompt
     */
    public void invalidateCache(GetPromptParam param) {
        String cacheKey = buildCacheKey(param);
        cache.invalidate(cacheKey);
    }
    
    /**
     * Invalidate all cached prompts.
     */
    public void invalidateAllCache() {
        cache.invalidateAll();
    }
    
    /**
     * Fetch prompt from server (called by cache loader).
     */
    private Prompt fetchPromptFromServer(String cacheKey) {
        try {
            // Parse cache key back to params
            // For simplicity, we'll reconstruct the request from cache key
            // In production, you might want a better approach
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("workspace_id", workspaceId);
            
            // Extract prompt_key from cache key
            String[] parts = cacheKey.split(":");
            if (parts.length > 0) {
                requestBody.put("prompt_key", parts[0]);
            }
            if (parts.length > 1 && !parts[1].isEmpty()) {
                requestBody.put("version", parts[1]);
            }
            if (parts.length > 2 && !parts[2].isEmpty()) {
                requestBody.put("label", parts[2]);
            }
            
            logger.debug("Fetching prompt from server: {}", cacheKey);
            
            String response = httpClient.post(promptEndpoint, requestBody);
            
            // Parse response
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = JsonUtils.fromJson(response, Map.class);
            
            if (responseMap.containsKey("data")) {
                String dataJson = JsonUtils.toJson(responseMap.get("data"));
                return JsonUtils.fromJson(dataJson, Prompt.class);
            } else {
                throw new PromptException(ErrorCode.PROMPT_NOT_FOUND,
                    "Prompt not found in response");
            }
        } catch (PromptException e) {
            throw e;
        } catch (Exception e) {
            throw new PromptException(ErrorCode.INTERNAL_ERROR,
                "Failed to fetch prompt from server", e);
        }
    }
    
    /**
     * Build cache key from parameters.
     */
    private String buildCacheKey(GetPromptParam param) {
        StringBuilder key = new StringBuilder(param.getPromptKey());
        key.append(":");
        key.append(param.getVersion() != null ? param.getVersion() : "");
        key.append(":");
        key.append(param.getLabel() != null ? param.getLabel() : "");
        return key.toString();
    }
}

