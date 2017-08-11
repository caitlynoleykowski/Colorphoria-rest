/*
 * Copyright (c) 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not  use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.example.echo;

import com.google.api.server.spi.auth.EspAuthenticator;
import com.google.api.server.spi.auth.common.User;
import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiIssuer;
import com.google.api.server.spi.config.ApiIssuerAudience;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.datastore.*;

import java.util.ArrayList;
import java.util.List;

/** The Echo API which Endpoints will be exposing. */
// [START echo_api_annotation]
@Api(
    name = "echo",
    version = "v1",
    namespace =
      @ApiNamespace(
        ownerDomain = "echo.example.com",
        ownerName = "echo.example.com",
        packagePath = ""
      ),
    // [START_EXCLUDE]
    issuers = {
      @ApiIssuer(
        name = "firebase",
        issuer = "https://securetoken.google.com/YOUR-PROJECT-ID",
        jwksUri = "https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com")
    }
    // [END_EXCLUDE]
    )
// [END echo_api_annotation]



public class Echo {


  /**
   * Echoes the received message back. If n is a non-negative integer, the message is copied that
   * many times in the returned message.
   *
   * Note that name is specified and will override the default name of "{class name}.{method
   * name}". For example, the default is "echo.echo".
   *
   * Note that httpMethod is not specified. This will default to a reasonable HTTP method
   * depending on the API method name. In this case, the HTTP method will default to POST.
   */
  // [START echo_method]
  @ApiMethod(name = "echo")
  public Message echo(Message message, @Named("n") @Nullable Integer n) {

    return doEcho(message, n);
  }
  // [END echo_method]

  /**
   * Echoes the received message back. If n is a non-negative integer, the message is copied that
   * many times in the returned message.
   *
   * Note that name is specified and will override the default name of "{class name}.{method
   * name}". For example, the default is "echo.echo".
   *
   * Note that httpMethod is not specified. This will default to a reasonable HTTP method
   * depending on the API method name. In this case, the HTTP method will default to POST.
   */
  // [START echo_path]
  @ApiMethod(name = "echo_path_parameter",
          path = "echo/{n}")
  public Message echoPathParameter(Message message, @Named("n") int n) {

    return doEcho(message, n);
  }
  // [END echo_path]

  /**
   * Echoes the received message back. If n is a non-negative integer, the message is copied that
   * many times in the returned message.
   *
   * Note that name is specified and will override the default name of "{class name}.{method
   * name}". For example, the default is "echo.echo".
   *
   * Note that httpMethod is not specified. This will default to a reasonable HTTP method
   * depending on the API method name. In this case, the HTTP method will default to POST.
   */
  // [START echo_api_key]
  @ApiMethod(name = "echo_api_key",
          path = "echo_api_key",
          apiKeyRequired = AnnotationBoolean.TRUE)
  public Message echoApiKey(Message message, @Named("n") @Nullable Integer n) {

    return doEcho(message, n);
  }
  // [END echo_api_key]





  private Message doEcho(Message message, Integer n) {
    if (n != null && n >= 0) {
      StringBuilder sb = new StringBuilder("Cait\'s");
      for (int i = 0; i < n; i++) {
        if (i > 0) {
          sb.append(" ");
        }
        sb.append(message.getMessage());
      }
      message.setMessage(sb.toString());
    }
    return message;
  }












  // [START record_upload]
  @ApiMethod(name = "record_upload",
          path = "record_upload",
          apiKeyRequired = AnnotationBoolean.TRUE)
  public Message recordUpload(
          @Named("user_id") @Nullable String userId,
          @Named("download_url") @Nullable String downloadUrl,
          @Named("privacy") @Nullable String privacy,
          @Named("comment") @Nullable String comment
          ) {


    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    Entity uploadedMedia = new Entity("UploadedMedia");
    uploadedMedia.setIndexedProperty("userId", userId);
    uploadedMedia.setUnindexedProperty("downloadUrl", downloadUrl);
    uploadedMedia.setIndexedProperty("privacy", privacy);
    uploadedMedia.setUnindexedProperty("comment", comment);

    datastore.put(uploadedMedia);


    Message returnMessage = new Message();
    returnMessage.setMessage(downloadUrl);
    return returnMessage;
  }
  // [END record_upload]




  // [START my_images]
  @ApiMethod(name = "my_images",
          path = "my_images",
          apiKeyRequired = AnnotationBoolean.TRUE)
  public MyImagesResult myImages(
          @Named("user_id") @Nullable String userId) {


    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    Query.Filter propertyFilter =
            new Query.FilterPredicate("userId", Query.FilterOperator.EQUAL, userId);
    Query photoQuery = new Query("UploadedMedia").setFilter(propertyFilter);

    // This returns weddingPhoto, babyPhoto, and dancePhoto,
    // but not campingPhoto, because tom is not an ancestor
    List<Entity> results = datastore.prepare(photoQuery).asList(FetchOptions.Builder.withDefaults());

    List<String> urls = new ArrayList<>();
    for (Entity result : results) {
        urls.add((String) result.getProperty("downloadUrl"));
    }

    MyImagesResult myImages = new MyImagesResult();
    myImages.setUrls(urls);

    return myImages;
  }
  // [END my_images]












  /**
   * Gets the authenticated user's email. If the user is not authenticated, this will return an HTTP
   * 401.
   *
   * Note that name is not specified. This will default to "{class name}.{method name}". For
   * example, the default is "echo.getUserEmail".
   *
   * Note that httpMethod is not required here. Without httpMethod, this will default to GET due
   * to the API method name. httpMethod is added here for example purposes.
   */
  // [START google_id_token_auth]
  @ApiMethod(
      httpMethod = ApiMethod.HttpMethod.GET,
      authenticators = {EspAuthenticator.class},
      audiences = {"YOUR_OAUTH_CLIENT_ID"},
      clientIds = {"YOUR_OAUTH_CLIENT_ID"}
      )
  public Email getUserEmail(User user) throws UnauthorizedException {
    if (user == null) {
      throw new UnauthorizedException("Invalid credentials");
    }

    Email response = new Email();
    response.setEmail(user.getEmail());
    return response;
  }
  // [END google_id_token_auth]

  /**
   * Gets the authenticated user's email. If the user is not authenticated, this will return an HTTP
   * 401.
   *
   * Note that name is not specified. This will default to "{class name}.{method name}". For
   * example, the default is "echo.getUserEmail".
   *
   * Note that httpMethod is not required here. Without httpMethod, this will default to GET due
   * to the API method name. httpMethod is added here for example purposes.
   */
  // [START firebase_auth]
  @ApiMethod(
      path = "firebase_user",
      httpMethod = ApiMethod.HttpMethod.GET,
      authenticators = {EspAuthenticator.class},
      issuerAudiences = {@ApiIssuerAudience(name = "firebase", audiences = {"YOUR-PROJECT-ID"})}
      )
  public Email getUserEmailFirebase(User user) throws UnauthorizedException {
    if (user == null) {
      throw new UnauthorizedException("Invalid credentials");
    }

    Email response = new Email();
    response.setEmail(user.getEmail());
    return response;
  }
  // [END firebase_auth]
}
