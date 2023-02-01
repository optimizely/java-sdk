/**
 *    Copyright 2022, Optimizely Inc. and contributors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.optimizely.ab.odp;

import com.optimizely.ab.OptimizelyHttpClient;
import com.optimizely.ab.annotations.VisibleForTesting;
import com.optimizely.ab.odp.parser.ResponseJsonParser;
import com.optimizely.ab.odp.parser.ResponseJsonParserFactory;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class DefaultODPApiManager implements ODPApiManager {
    private static final Logger logger = LoggerFactory.getLogger(DefaultODPApiManager.class);

    private final OptimizelyHttpClient httpClientSegments;
    private final OptimizelyHttpClient httpClientEvents;

    public DefaultODPApiManager() {
        this(OptimizelyHttpClient.builder().build());
    }

    public DefaultODPApiManager(int segmentFetchTimeoutMillis, int eventDispatchTimeoutMillis) {
        httpClientSegments = OptimizelyHttpClient.builder().setTimeoutMillis(segmentFetchTimeoutMillis).build();
        if (segmentFetchTimeoutMillis == eventDispatchTimeoutMillis) {
            // If the timeouts are same, single httpClient can be used for both.
            httpClientEvents = httpClientSegments;
        } else {
            httpClientEvents = OptimizelyHttpClient.builder().setTimeoutMillis(eventDispatchTimeoutMillis).build();
        }
    }

    @VisibleForTesting
    DefaultODPApiManager(OptimizelyHttpClient httpClient) {
        this.httpClientSegments = httpClient;
        this.httpClientEvents = httpClient;
    }

    @VisibleForTesting
    String getSegmentsStringForRequest(Set<String> segmentsList) {

        StringBuilder segmentsString = new StringBuilder();
        Iterator<String> segmentsListIterator = segmentsList.iterator();
        for (int i = 0; i < segmentsList.size(); i++) {
            if (i > 0) {
                segmentsString.append(", ");
            }
            segmentsString.append("\"").append(segmentsListIterator.next()).append("\"");
        }
        return segmentsString.toString();
    }

    // ODP GraphQL API
    // - https://api.zaius.com/v3/graphql
    // - test ODP public API key = "W4WzcEs-ABgXorzY7h1LCQ"
    /*

     [GraphQL Request]

     // fetch info with fs_user_id for ["has_email", "has_email_opted_in", "push_on_sale"] segments
     curl -i -H 'Content-Type: application/json' -H 'x-api-key: W4WzcEs-ABgXorzY7h1LCQ' -X POST -d '{"query":"query {customer(fs_user_id: \"tester-101\") {audiences(subset:[\"has_email\",\"has_email_opted_in\",\"push_on_sale\"]) {edges {node {name state}}}}}"}' https://api.zaius.com/v3/graphql
     // fetch info with vuid for ["has_email", "has_email_opted_in", "push_on_sale"] segments
     curl -i -H 'Content-Type: application/json' -H 'x-api-key: W4WzcEs-ABgXorzY7h1LCQ' -X POST -d '{"query":"query {customer(vuid: \"d66a9d81923d4d2f99d8f64338976322\") {audiences(subset:[\"has_email\",\"has_email_opted_in\",\"push_on_sale\"]) {edges {node {name state}}}}}"}' https://api.zaius.com/v3/graphql
     query MyQuery {
       customer(vuid: "d66a9d81923d4d2f99d8f64338976322") {
         audiences(subset:["has_email","has_email_opted_in","push_on_sale"]) {
           edges {
             node {
               name
               state
             }
           }
         }
       }
     }
     [GraphQL Response]

     {
       "data": {
         "customer": {
           "audiences": {
             "edges": [
               {
                 "node": {
                   "name": "has_email",
                   "state": "qualified",
                 }
               },
               {
                 "node": {
                   "name": "has_email_opted_in",
                   "state": "qualified",
                 }
               },
                ...
             ]
           }
         }
       }
     }

     [GraphQL Error Response]
     {
       "errors": [
         {
           "message": "Exception while fetching data (/customer) : java.lang.RuntimeException: could not resolve _fs_user_id = asdsdaddddd",
           "locations": [
             {
               "line": 2,
               "column": 3
             }
           ],
           "path": [
             "customer"
           ],
           "extensions": {
             "classification": "InvalidIdentifierException"
           }
         }
       ],
       "data": {
         "customer": null
       }
     }
    */
    @Override
    public List<String> fetchQualifiedSegments(String apiKey, String apiEndpoint, String userKey, String userValue, Set<String> segmentsToCheck) {
        HttpPost request = new HttpPost(apiEndpoint);
        String segmentsString = getSegmentsStringForRequest(segmentsToCheck);

        String query = String.format("query($userId: String, $audiences: [String]) {customer(%s: $userId) {audiences(subset: $audiences) {edges {node {name state}}}}}", userKey);
        String variables = String.format("{\"userId\": \"%s\", \"audiences\": [%s]}", userValue, segmentsString);
        String requestPayload = String.format("{\"query\": \"%s\", \"variables\": %s}", query, variables);

        try {
            request.setEntity(new StringEntity(requestPayload));
        } catch (UnsupportedEncodingException e) {
            logger.warn("Error encoding request payload", e);
        }
        request.setHeader("x-api-key", apiKey);
        request.setHeader("content-type", "application/json");

        CloseableHttpResponse response = null;
        try {
            response = httpClientSegments.execute(request);
        } catch (IOException e) {
            logger.error("Error retrieving response from ODP service", e);
            return null;
        }

        if (response.getStatusLine().getStatusCode() >= 400) {
            StatusLine statusLine = response.getStatusLine();
            logger.error(String.format("Unexpected response from ODP server, Response code: %d, %s", statusLine.getStatusCode(), statusLine.getReasonPhrase()));
            closeHttpResponse(response);
            return null;
        }
        ResponseJsonParser parser = ResponseJsonParserFactory.getParser();
        try {
            return parser.parseQualifiedSegments(EntityUtils.toString(response.getEntity()));
        } catch (IOException e) {
            logger.error("Error converting ODP segments response to string", e);
        } catch (Exception e) {
            logger.error("Audience segments fetch failed (Error Parsing Response)");
            logger.debug(e.getMessage());
        }finally {
            closeHttpResponse(response);
        }
        return null;
    }

    /*
    eventPayload Format


    Returns:
    1. null, When there was a non-recoverable error and no retry is needed.
    2. 0 If an unexpected error occurred and retrying can be useful.
    3. HTTPStatus code if httpclient was able to make the request and was able to receive response.
       It is recommended to retry if status code was 5xx.
     */
    @Override
    public Integer sendEvents(String apiKey, String apiEndpoint, String eventPayload) {
        HttpPost request = new HttpPost(apiEndpoint);

        try {
            request.setEntity(new StringEntity(eventPayload));
        } catch (UnsupportedEncodingException e) {
            logger.error("ODP event send failed (Error encoding request payload)", e);
            return null;
        }
        request.setHeader("x-api-key", apiKey);
        request.setHeader("content-type", "application/json");

        CloseableHttpResponse response = null;
        try {
            response = httpClientEvents.execute(request);
        } catch (IOException e) {
            logger.error("Error retrieving response from event request", e);
            return 0;
        }

        int statusCode = response.getStatusLine().getStatusCode();
        if ( statusCode >= 400) {
            StatusLine statusLine = response.getStatusLine();
            logger.error(String.format("ODP event send failed (Response code: %d, %s)", statusLine.getStatusCode(), statusLine.getReasonPhrase()));
        } else {
            logger.debug("ODP Event Dispatched successfully");
        }

        closeHttpResponse(response);
        return statusCode;
    }

    private static void closeHttpResponse(CloseableHttpResponse response) {
        if (response != null) {
            try {
                response.close();
            } catch (IOException e) {
                logger.warn(e.getLocalizedMessage());
            }
        }
    }
}
