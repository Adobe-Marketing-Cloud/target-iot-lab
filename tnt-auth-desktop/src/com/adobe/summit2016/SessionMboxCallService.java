package com.adobe.summit2016;


import javafx.scene.control.TextArea;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.Map;

/**
 * @author Adobe Systems Inc.
 */
public class SessionMboxCallService {

    private final String thirdPartyId;
    private final String clientCode;
    private final TextArea debugArea;

    private String edgeHost;

    public SessionMboxCallService(String thirdPartyId, TextArea debugArea) {
        this.debugArea = debugArea;
        this.thirdPartyId = thirdPartyId;
        this.clientCode = StringUtils.defaultString(System.getProperty("clientCode"), "adobesummit021");
    }

    public String getContent(String mbox, Map<String, String> mboxParameters) throws TntApiCallException {
        String host = StringUtils.defaultString(edgeHost, clientCode + ".tt.omtrdc.net");
        String url = "http://" + host + "/rest/v1/mbox/" + thirdPartyId +
                "?client=" + clientCode;
        try {
            URL urlToRequest = new URL(url);
            HttpURLConnection urlConnection = (HttpURLConnection) urlToRequest.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/json");
            JSONObject mboxRequestJson = new JSONObject();
            mboxRequestJson.put("mbox", mbox);
            mboxRequestJson.put("thirdPartyId", thirdPartyId);
            mboxRequestJson.put("mboxParameters", new JSONObject(mboxParameters));
            DataOutputStream dStream = new DataOutputStream(urlConnection.getOutputStream());
            dStream.writeBytes(mboxRequestJson.toString());
            dStream.flush();
            dStream.close();

            debugArea.appendText(new Date().toString() + ": \n");
            debugArea.appendText("POST to " + url + " " + mboxRequestJson.toString());
            debugArea.appendText("\n");
            debugArea.appendText("\n");

            int responseCode = urlConnection.getResponseCode();
            if (responseCode != 200) {
                StringWriter writer = new StringWriter();
                IOUtils.copy(urlConnection.getErrorStream(), writer);
                throw new TntApiCallException(responseCode + " " + writer.toString());
            }

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String line;
            StringBuilder responseBuilder = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                responseBuilder.append(line);
            }

            String response = responseBuilder.toString();

            debugArea.appendText("Response from Target: " + response);
            debugArea.appendText("\n");
            debugArea.appendText("\n");

            JSONObject jsonResponse = new JSONObject(response);
            this.edgeHost = jsonResponse.getString("edgeHost");
            return jsonResponse.getString("content");
        } catch (Exception e) {
            throw new TntApiCallException("Exception while making a call to TNT. Message: " + e.getMessage());
        }
    }
}
