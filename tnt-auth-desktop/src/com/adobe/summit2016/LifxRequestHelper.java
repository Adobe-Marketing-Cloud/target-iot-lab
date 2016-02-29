package com.adobe.summit2016;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Adobe Systems Inc.
 */
public class LifxRequestHelper {

  private LifxRequestHelper() {
  }

  public static Map<String, String> setLighBulbStates(String content) throws LifxApiCallException {
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
      JSONObject jsonResponse = new JSONObject(response);
      JSONArray lightBulbs = jsonResponse.getJSONArray("results");
      Map<String, String> statuses = new HashMap<>();
      for (int index = 0; index < lightBulbs.length(); index++) {
        JSONObject bulb = lightBulbs.getJSONObject(index);
        statuses.put(bulb.getString("id"), bulb.getString("status"));
      }

      return statuses;
    } catch (Exception e) {
      throw new LifxApiCallException("Exception while making a call to Lifx. Message: " + e.getMessage());
    }
  }

}
