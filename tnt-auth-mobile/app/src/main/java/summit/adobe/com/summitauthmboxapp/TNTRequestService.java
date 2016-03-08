package summit.adobe.com.summitauthmboxapp;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * @author Adobe Systems Inc.
 */
public class TNTRequestService {

  private final String thirdPartyId;
  private final String clientCode;
  private final MainActivity activity;

  private String edgeHost;

  public TNTRequestService(String thirdPartyId, MainActivity activity) {
    this.thirdPartyId = thirdPartyId;
    this.clientCode = "adobesummit021";
    this.activity = activity;
  }

  public String getContent(String mbox, Map<String, String> mboxParameters,
                           Map<String, String> profileParameters) throws TntApiCallException {
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
      mboxRequestJson.put("profileParameters", new JSONObject(profileParameters));
      DataOutputStream dStream = new DataOutputStream(urlConnection.getOutputStream());
      dStream.writeBytes(mboxRequestJson.toString());
      dStream.flush();
      dStream.close();

      activity.debug("POST to " + url + " " + mboxRequestJson.toString());

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

      activity.debug("Response from Target: " + response);

      JSONObject jsonResponse = new JSONObject(response);
      this.edgeHost = jsonResponse.getString("edgeHost");
      return jsonResponse.getString("content");
    } catch (Exception e) {
      throw new TntApiCallException("Exception while making a call to TNT. Message: " + e.getMessage());
    }
  }

  public void updateProfile(Map<String, String> profileParameters) throws TntApiCallException {
    if (profileParameters == null || profileParameters.size() == 0) {
      activity.debug("Not making profile update call to Target since profileParameters is empty");
      return;
    }

    StringBuilder parametersString = new StringBuilder();
    for (Map.Entry<String, String> parameter : profileParameters.entrySet()) {
      parametersString.append("&").append(parameter.getKey()).append("=").append(parameter.getValue());
    }

    String host = StringUtils.defaultString(edgeHost, clientCode + ".tt.omtrdc.net");
    String url = "http://" + host + "/m2/demo/profile/update?mbox3rdPartyId=" + thirdPartyId + parametersString;
    try {
      URL urlToRequest = new URL(url);
      HttpURLConnection urlConnection = (HttpURLConnection) urlToRequest.openConnection();
      urlConnection.setDoOutput(true);
      urlConnection.setRequestMethod("GET");

      activity.debug("GET to " + url);

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

      activity.debug("Response from Target: " + response);
    } catch (Exception e) {
      throw new TntApiCallException("Exception while making a call to TNT. Message: " + e.getMessage());
    }
  }

}
