package com.adobe.summit2016;

import javafx.scene.control.TextArea;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author Adobe Systems Inc.
 */
public class LifxRequestHelper {

    private final TextArea debugArea;

    public LifxRequestHelper(TextArea debugArea) {
        this.debugArea = debugArea;
    }

    public void setLightBulbStates(String content) throws LifxApiCallException {
        try {
            URL urlToRequest = new URL("https://api.lifx.com/v1/lights/all/state");
            HttpURLConnection urlConnection = (HttpURLConnection) urlToRequest.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("PUT");
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("Authorization",
                    "Bearer c916a1616888be1886cbd1e5d7143a46525d2eb94dd37413ffa9f40c3103d136");

            DataOutputStream dStream = new DataOutputStream(urlConnection.getOutputStream());
            dStream.writeBytes(content);
            dStream.flush();
            dStream.close();

            int responseCode = urlConnection.getResponseCode();
            if (responseCode != 207) {
                StringWriter writer = new StringWriter();
                IOUtils.copy(urlConnection.getErrorStream(), writer);
                throw new LifxApiCallException(responseCode + " " + writer.toString());
            }

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String line;
            StringBuilder responseBuilder = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                responseBuilder.append(line);
            }

            String response = responseBuilder.toString();

            debugArea.appendText("Response from Lifx: " + response);
            debugArea.appendText("\n");
            debugArea.appendText("\n");
        } catch (Exception e) {
            throw new LifxApiCallException("Exception while making a call to Lifx. Message: " + e.getMessage());
        }
    }

}
