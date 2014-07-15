/*
   Copyright 2007 Fourspaces Consulting, LLC

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.fourspaces.couchdb;

import java.io.IOException;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.util.EntityUtils;

/**
 * The CouchResponse parses the HTTP response returned by the CouchDB server.
 * This is almost never called directly by the user, but indirectly through
 * the Session and Database objects.
 * <p>
 * Given a CouchDB response, it will determine if the request was successful
 * (status 200,201,202), or was an error.  If there was an error, it parses the returned json error
 * message.
 *
 * @author mbreese
 */
public class CouchResponse {
  Log log = LogFactory.getLog(CouchResponse.class);

  private String body;
  private String path;
  private Header[] headers;
  private int statusCode;
  private String phrase;
  private String methodName;
  boolean ok = false;

  private String errorId;
  private String errorReason;

  /**
   * C-tor parses the method results to build the CouchResponse content.
   * First, it reads the body (hence the IOException) from the method
   * Next, it checks the status codes to determine if the request was successful.
   * If there was an error, it parses the error codes.
   *
   * @throws IOException
   */
  CouchResponse(HttpRequestBase req, HttpResponse response) throws IOException {
    headers = response.getAllHeaders();

    HttpEntity entity = response.getEntity();
    body = EntityUtils.toString(entity);

    path = req.getURI().getPath();

    statusCode = response.getStatusLine().getStatusCode();
    phrase = response.getStatusLine().getReasonPhrase();

    log.info("Status code: " + statusCode);
    String statusCodeStr = String.valueOf(statusCode);
    if (statusCodeStr.startsWith("2")) {
      if (path.endsWith("_bulk_docs")) { // Handle bulk doc update differently
        ok = JSONArray.fromObject(body).size() > 0;
      } else {
        ok = JSONObject.fromObject(body).getBoolean("ok");
      }
    } else {
      // Assume error
      JSONObject jbody = JSONObject.fromObject(body);
      errorId = jbody.getString("error");
      errorReason = jbody.getString("reason");
    }

    log.debug(toString());
  }

  @Override
  /**
   * A better toString for this content... can be very verbose though.
   */
  public String toString() {
    return "[" + methodName + "] " + path + " [" + statusCode + "] " + " => " + body;
  }

  /**
   * Retrieves the body of the request as a JSONArray content. (such as listing database names)
   *
   * @return
   */
  public JSONArray getBodyAsJSONArray() {
    if (body == null) return null;
    return JSONArray.fromObject(body);
  }

  /**
   * Was the request successful?
   *
   * @return
   */
  public boolean isOk() {
    return ok;
  }

  /**
   * What was the error id?
   *
   * @return
   */
  public String getErrorId() {
    return errorId;
  }

  /**
   * what was the error reason given?
   *
   * @return
   */
  public String getErrorReason() {
    return errorReason;
  }

  /**
   * Returns the body of the response as a JSON Object (such as for a document)
   *
   * @return
   */
  public JSONObject getBodyAsJSONObject() {
    if (body == null) {
      return null;
    }
    return JSONObject.fromObject(body);
  }


  /**
   * Retrieves a specific header from the response (not really used anymore)
   *
   * @param key
   * @return
   */
  public String getHeader(String key) {
    for (Header h : headers) {
      if (h.getName().equals(key)) {
        return h.getValue();
      }
    }
    return null;
  }

  public String getBody() {
    return body;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(int statusCode) {
    this.statusCode = statusCode;
  }

  public String getPhrase() {
    return phrase;
  }

  public void setPhrase(String phrase) {
    this.phrase = phrase;
  }
}
