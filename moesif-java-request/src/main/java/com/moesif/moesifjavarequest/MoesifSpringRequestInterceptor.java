package com.moesif.moesifjavarequest;

import com.moesif.api.models.*;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import com.moesif.api.MoesifAPIClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MoesifSpringRequestInterceptor implements ClientHttpRequestInterceptor {
    private MoesifAPIClient moesifApi;

    public MoesifSpringRequestInterceptor(MoesifAPIClient moesifApi) {
        this.moesifApi = moesifApi;
    }

    public MoesifSpringRequestInterceptor(String applicationId) {
        this.moesifApi = new MoesifAPIClient(applicationId);
    }

    private String streamToString(InputStream inputStream) {
        try {
            StringBuilder inputStringBuilder = new StringBuilder();
            // TODO: different encodings?
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            String line = bufferedReader.readLine();

            while (line != null) {
                inputStringBuilder.append(line);
                inputStringBuilder.append('\n');
                line = bufferedReader.readLine();
            }

            return inputStringBuilder.toString();
        } catch (IOException e) {
            return "";
        }
    }

    private EventRequestModel buildEventRequestModel(HttpRequest request, byte[] body) {
        // TODO: transactionId
        EventRequestBuilder eventRequestBuilder = new EventRequestBuilder();
        Map<String, String> reqHeaders = new HashMap<String, String>(0);
        eventRequestBuilder
                .time(new Date())
                .uri(request.getURI().toString())
                .headers(request.getHeaders().toSingleValueMap())
                .verb(request.getMethod().toString());
                // .ipAddress()

        // TODO: apiVersion
        eventRequestBuilder.body(new String(body)); // TODO: need transfer encoding still?

        return eventRequestBuilder.build();
    }

    private EventResponseModel buildEventResponseModel(ClientHttpResponse response) {
        EventResponseBuilder eventResponseBuilder = new EventResponseBuilder();

        int statusCode = 0;

        try {
            statusCode = response.getRawStatusCode();
        } catch (Exception e) {
            System.out.println("Error getting raw status code");
        }

        eventResponseBuilder
                .time(new Date())
                .status(statusCode)
                .headers(response.getHeaders().toSingleValueMap());

        // response.getBody
        try {
            // eventResponseBuilder.body(response.getBody()); // TODO: Definitely doesn't work
            eventResponseBuilder.body(streamToString(response.getBody())); // TODO: verify input can still be read
        } catch (Exception e) {
            System.out.println("Error getting response body");
        }

        return eventResponseBuilder.build();
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        System.out.println("intercept()");
        System.out.println(request.getURI());
        System.out.println(request.getMethod());

//        config.identifyUser(httpRequest, httpResponse),
//        config.getSessionToken(httpRequest, httpResponse),
//        config.getTags(httpRequest, httpResponse),
//        config.getMetadata(httpRequest, httpResponse)

        AppConfigModel appConfig = moesifApi.getAPI().getCachedAndLoadAppConfig();


        EventRequestModel eventRequestModel = buildEventRequestModel(request, body);

        // run the request
        ClientHttpResponse response = execution.execute(request, body);

        EventResponseModel eventResponseModel = buildEventResponseModel(response);

        System.out.println("begin SendEvent()");
        moesifApi.getAPI().sendEvent(
                eventRequestModel,
                eventResponseModel,
                null,
                null,
                null,
                null
        );
        System.out.println("end SendEvent()");

        return response;
    }
}

