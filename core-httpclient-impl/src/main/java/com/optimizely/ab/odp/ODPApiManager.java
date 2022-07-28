package com.optimizely.ab.odp;

import com.optimizely.ab.event.AsyncEventHandler;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

public class ODPApiManager {

    private static final Logger logger = LoggerFactory.getLogger(ODPApiManager.class);

    private String getSegmentsStringForRequest(List<String> segmentsList) {
        StringBuilder segmentsString = new StringBuilder();
        for (int i = 0; i < segmentsList.size(); i++) {
            if (i > 0) {
                segmentsString.append(", ");
            }
            segmentsString.append("\\\"").append(segmentsList.get(i)).append("\\\"");
        }
        return segmentsString.toString();
    }

    public String fetchQualifiedSegments(String apiKey, String apiEndpoint, String userKey, String userValue, List<String> segmentsToCheck) {
        HttpPost request = new HttpPost(apiEndpoint);
        String segmentsString = getSegmentsStringForRequest(segmentsToCheck);
        String requestPayload = String.format("{\"query\": \"query {customer(%s: \\\"%s\\\") {audiences(subset: [%s]) {edges {node {name state}}}}}\"}", userKey, userValue, segmentsString);

        try {
            request.setEntity(new StringEntity(requestPayload));
        } catch (UnsupportedEncodingException e) {
            logger.warn("Error encoding request payload", e);
        }
        request.setHeader("x-api-key", apiKey);
        request.setHeader("content-type", "application/json");
        HttpClient client = HttpClientBuilder.create().build();


        HttpResponse response = null;
        try {
            response = client.execute(request);
        } catch (IOException e) {
            logger.error("Error retrieving response from ODP service", e);
            return null;
        }

        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            StatusLine statusLine = response.getStatusLine();
            logger.error(String.format("Unexpected response from ODP server, Response code: %d, %s", statusLine.getStatusCode(), statusLine.getReasonPhrase()));
            return null;
        }

        try {
            return EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            logger.error("Error converting ODP segments response to string", e);
        }
        return null;
    }
}
