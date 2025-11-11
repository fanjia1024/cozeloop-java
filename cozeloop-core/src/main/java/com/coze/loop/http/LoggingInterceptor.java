package com.coze.loop.http;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Interceptor to log HTTP requests and responses.
 */
public class LoggingInterceptor implements Interceptor {
    private static final Logger logger = LoggerFactory.getLogger(LoggingInterceptor.class);
    
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        
        long startTime = System.currentTimeMillis();
        logger.debug("Sending request: {} {}", request.method(), request.url());
        
        Response response = chain.proceed(request);
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Log response (without consuming body)
        ResponseBody body = response.body();
        String bodyString = body != null ? body.string() : "";
        
        logger.debug("Received response: {} {} in {}ms, status: {}, body length: {}",
            request.method(), request.url(), duration, response.code(), bodyString.length());
        
        // Recreate response with the body we just read
        Response newResponse = response.newBuilder()
            .body(ResponseBody.create(bodyString, body != null ? body.contentType() : null))
            .build();
        
        return newResponse;
    }
}

