
/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.outlook.mail;

import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthState;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.exoplatform.outlook.OutlookUser;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.ws.frameworks.json.JsonParser;
import org.exoplatform.ws.frameworks.json.impl.JsonDefaultHandler;
import org.exoplatform.ws.frameworks.json.impl.JsonException;
import org.exoplatform.ws.frameworks.json.impl.JsonParserImpl;
import org.exoplatform.ws.frameworks.json.value.JsonValue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;

/**
 * Java client for <a href="https://msdn.microsoft.com/office/office365/api/mail-rest-operations">Outlook Mail
 * REST API</a>. <br>
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: MailAPI.java 00000 Jun 7, 2016 pnedonosko $
 * 
 */
public class MailAPI {

  /** The Constant LOG. */
  protected static final Log          LOG                   = ExoLogger.getLogger(MailAPI.class);

  /** The Constant READ_ATTACHMENT_ERROR. */
  protected static final String       READ_ATTACHMENT_ERROR = "Error requesting message attachment";
  
  /** The Constant READ_MESSAGE_ERROR. */
  protected static final String       READ_MESSAGE_ERROR = "Error requesting message";

  /** The Constant MODELS_ERROR. */
  protected static final String       MODELS_ERROR          = "Error requesting workspace models";

  /** The http client. */
  protected final CloseableHttpClient httpClient;

  /** The http context. */
  protected final HttpClientContext   httpContext;

  /** The accept json header. */
  protected final Header              acceptJsonHeader;

  /**
   * Instantiates a new mail API.
   *
   * @param httpClient the http client
   * @throws MailServerException the mail server exception
   */
  MailAPI(CloseableHttpClient httpClient) throws MailServerException {

    if (httpClient == null) {
      // FYI it's possible make more advanced conn manager settings (host verification X509, conn config,
      // message parser etc.)
      PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
      // 2 recommended by RFC 2616 sec 8.1.4, we make it bigger for quicker // upload
      connectionManager.setDefaultMaxPerRoute(10);
      connectionManager.setMaxTotal(100);

      // Create global request configuration
      RequestConfig defaultRequestConfig = RequestConfig.custom()
                                                        .setExpectContinueEnabled(true)
                                                        .setStaleConnectionCheckEnabled(true)
                                                        .setAuthenticationEnabled(true)
                                                        .setTargetPreferredAuthSchemes(Arrays.asList(AuthSchemes.BASIC))
                                                        // .setProxyPreferredAuthSchemes(Arrays.asList(AuthSchemes.BASIC))
                                                        // .setCookieSpec(CookieSpecs.BEST_MATCH)
                                                        .build();

      // Create HTTP client
      this.httpClient = HttpClients.custom()
                                   .setConnectionManager(connectionManager)
                                   // .setDefaultCredentialsProvider(credsProvider)
                                   .setDefaultRequestConfig(defaultRequestConfig)
                                   .build();
    } else {
      // Use given HTTP client (for tests)
      this.httpClient = httpClient;
    }

    // Default header (Accept JSON), add to those requests where required
    this.acceptJsonHeader = new BasicHeader("Accept", ContentType.APPLICATION_JSON.getMimeType());

    // Add AuthCache to the execution context
    this.httpContext = HttpClientContext.create();
  }

  /**
   * Instantiates a new mail API.
   *
   * @throws MailServerException the mail server exception
   */
  public MailAPI() throws MailServerException {
    this(null);
  }

  /**
   * Close.
   *
   * @throws MailServerException the mail server exception
   */
  public void close() throws MailServerException {
    try {
      this.httpClient.close();
    } catch (IOException e) {
      throw new MailServerException("Error closing HTTP client", e);
    }
  }

  /**
   * Reset.
   */
  public void reset() {
    AuthState authState = httpContext.getTargetAuthState();
    if (authState != null) {
      authState.reset();
    }
  }

  /**
   * Gets the attachment.
   *
   * @param user the user
   * @param messageId the message id
   * @param attachmentToken the attachment token
   * @param attachmentId the attachment id
   * @return the attachment
   * @throws BadCredentialsException the bad credentials exception
   * @throws ForbiddenException the forbidden exception
   * @throws MailServerException the mail server exception
   */
  public JsonValue getAttachment(OutlookUser user,
                                 String messageId,
                                 String attachmentToken,
                                 String attachmentId) throws BadCredentialsException,
                                                      ForbiddenException,
                                                      MailServerException {
    URI uri = user.getMailServerUrl()
                  .resolve(new StringBuilder("/api/v2.0/me/messages/").append(messageId)
                                                                      .append("/attachments/")
                                                                      .append(attachmentId)
                                                                      .toString());
    HttpGet get = new HttpGet(uri);
    get.setHeader(acceptJsonHeader);
    get.setHeader(new BasicHeader("Authorization", new StringBuilder("Bearer ").append(attachmentToken).toString()));
    get.setHeader(new BasicHeader("x-AnchorMailbox", user.getEmail()));

    if (LOG.isDebugEnabled()) {
      LOG.debug(">> getAttachment() " + get.getRequestLine());
    }
    try (CloseableHttpResponse response = httpClient.execute(get, httpContext);
        InputStream content = response.getEntity().getContent()) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("<< getAttachment() " + response.getStatusLine());
      }
      checkError(response, READ_ATTACHMENT_ERROR);
      // TODO should we care about redirects (proxy etc)?
      return readJson(content);
    } catch (JsonException e) {
      throw new MailServerException("Error parsing message attachment response", e);
    } catch (ClientProtocolException e) {
      throw new MailServerException("Error establishing message attachment request", e);
    } catch (IOException e) {
      throw new MailServerException("Error reading message attachment", e);
    }
  }

  /**
   * Gets the message.
   *
   * @param user the user
   * @param messageId the message id
   * @param messageToken the message token
   * @return the message
   * @throws BadCredentialsException the bad credentials exception
   * @throws ForbiddenException the forbidden exception
   * @throws MailServerException the mail server exception
   */
  public JsonValue getMessage(OutlookUser user,
                           String messageId,
                           String messageToken) throws BadCredentialsException, ForbiddenException, MailServerException {
    URI uri = user.getMailServerUrl().resolve(new StringBuilder("/api/v2.0/me/messages/").append(messageId).toString());
    HttpGet get = new HttpGet(uri);
    get.setHeader(acceptJsonHeader);
    get.setHeader(new BasicHeader("Authorization", new StringBuilder("Bearer ").append(messageToken).toString()));
    get.setHeader(new BasicHeader("x-AnchorMailbox", user.getEmail()));

    if (LOG.isDebugEnabled()) {
      LOG.debug(">> getMessage() " + get.getRequestLine());
    }
    try (CloseableHttpResponse response = httpClient.execute(get, httpContext);
        InputStream content = response.getEntity().getContent()) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("<< getMessage() " + response.getStatusLine());
      }
      checkError(response, READ_MESSAGE_ERROR);
      // TODO should we care about redirects (proxy etc)?
      return readJson(content);
    } catch (JsonException e) {
      throw new MailServerException("Error parsing message response", e);
    } catch (ClientProtocolException e) {
      throw new MailServerException("Error establishing message request", e);
    } catch (IOException e) {
      throw new MailServerException("Error reading message", e);
    }
  }

  // ******* internals *******

  /**
   * Read json.
   *
   * @param content the content
   * @return the json value
   * @throws JsonException the json exception
   */
  protected JsonValue readJson(InputStream content) throws JsonException {
    JsonParser jsonParser = new JsonParserImpl();
    JsonDefaultHandler handler = new JsonDefaultHandler();
    jsonParser.parse(new InputStreamReader(content, ContentType.APPLICATION_JSON.getCharset()), handler);
    return handler.getJsonObject();
  }

  /**
   * Read text.
   *
   * @param entity the entity
   * @return the string
   */
  protected String readText(HttpEntity entity) {
    String errorText;
    try {
      Charset encoding;
      Header encodingHeader = entity.getContentEncoding();
      if (encodingHeader != null) {
        encoding = Charset.forName(encodingHeader.getValue());
      } else {
        encoding = Consts.UTF_8;
      }
      errorText = EntityUtils.toString(entity, encoding);
    } catch (UnsupportedCharsetException e) {
      LOG.warn("Unsupported entity encoding", e);
      errorText = null;
    } catch (IllegalCharsetNameException e) {
      LOG.warn("Illageal entity encoding", e);
      errorText = null;
    } catch (IllegalArgumentException e) {
      LOG.warn("Entity encoding is null", e);
      errorText = null;
    } catch (ParseException e) {
      LOG.warn("Error parsing entity as text", e);
      errorText = null;
    } catch (IOException e) {
      LOG.warn("Error reading entity as text", e);
      errorText = null;
    }
    return errorText;
  }

  /**
   * Check error.
   *
   * @param response the response
   * @param errorBase the error base
   * @throws BadCredentialsException the bad credentials exception
   * @throws ForbiddenException the forbidden exception
   * @throws MailServerException the mail server exception
   */
  protected void checkError(HttpResponse response, String errorBase) throws BadCredentialsException,
                                                                     ForbiddenException,
                                                                     MailServerException {
    int status = response.getStatusLine().getStatusCode();
    if (status >= HttpStatus.SC_BAD_REQUEST) {
      // TODO 4xx vs 5xx?
      String errorText = readText(response.getEntity());
      String userInfo = ""; // FYI it's for debug only
      if (status == HttpStatus.SC_UNAUTHORIZED) {
        reset();
        if (LOG.isDebugEnabled()) {
          LOG.debug("Mail server authentication failure " + userInfo + errorText);
        }
        String message = new StringBuilder("Authentication failed. ").append(errorText).toString();
        throw new BadCredentialsException(message);
      } else if (status == HttpStatus.SC_FORBIDDEN) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Mail server access forbidden" + userInfo + errorText);
        }
        String message = new StringBuilder("Access forbidden. ").append(errorText).toString();
        throw new ForbiddenException(message);
      } else {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Mail server error" + userInfo + errorText);
        }
        String message = new StringBuilder(errorBase).append(". ").append(errorText).toString();
        throw new MailServerException(message);
      }
    }
  }
}
