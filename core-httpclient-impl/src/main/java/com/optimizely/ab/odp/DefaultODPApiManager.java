package com.optimizely.ab.odp;

import com.optimizely.ab.OptimizelyHttpClient;
import com.optimizely.ab.annotations.VisibleForTesting;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

public class DefaultODPApiManager implements ODPApiManager {
    private static final Logger logger = LoggerFactory.getLogger(DefaultODPApiManager.class);

    private final OptimizelyHttpClient httpClient;

    public DefaultODPApiManager() {
        this(OptimizelyHttpClient.builder().build());
    }

    @VisibleForTesting
    DefaultODPApiManager(OptimizelyHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @VisibleForTesting
    String getSegmentsStringForRequest(List<String> segmentsList) {
        StringBuilder segmentsString = new StringBuilder();
        for (int i = 0; i < segmentsList.size(); i++) {
            if (i > 0) {
                segmentsString.append(", ");
            }
            segmentsString.append("\\\"").append(segmentsList.get(i)).append("\\\"");
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

        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(request);
        } catch (IOException e) {
            logger.error("Error retrieving response from ODP service", e);
            return null;
        }

        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            StatusLine statusLine = response.getStatusLine();
            logger.error(String.format("Unexpected response from ODP server, Response code: %d, %s", statusLine.getStatusCode(), statusLine.getReasonPhrase()));
            closeHttpResponse(response);
            return null;
        }

        try {
            return EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            logger.error("Error converting ODP segments response to string", e);
        } finally {
            closeHttpResponse(response);
        }
        return null;
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
