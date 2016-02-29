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

  private String edgeHost;

  public TNTRequestService(String thirdPartyId) {
    this.thirdPartyId = thirdPartyId;
    this.clientCode = "adobeinternalsummitl";
  }

  public String getContent(String mbox, Map<String, String> mboxParameters,
                           Map<String, String> profileParameters) throws TntApiCallException {
    String host = StringUtils.defaultString(edgeHost, clientCode + ".tt.omtrdc.net");
    try {
      URL urlToRequest = new URL("http://" + host + "/rest/v1/mbox/" + thirdPartyId +
        "?client=" + clientCode);
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
      JSONObject jsonResponse = new JSONObject(response);
      this.edgeHost = jsonResponse.getString("edgeHost");
      return jsonResponse.getString("content");
    } catch (Exception e) {
      throw new TntApiCallException("Exception while making a call to TNT. Message: " + e.getMessage());
    }
  }
}
