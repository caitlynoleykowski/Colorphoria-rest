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

package com.caitoo0o.filler;

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

import java.security.InvalidParameterException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/** The MediaMetadata API which Endpoints will be exposing. */
// [START echo_api_annotation]
@Api(
    name = "mediametadata",
    version = "v1",
    namespace =
      @ApiNamespace(
        ownerDomain = "filler.caitoo0o.com",
        ownerName = "filler.caitoo0o.com",
        packagePath = ""
      ),
    // [START_EXCLUDE]
    issuers = {
      @ApiIssuer(
        name = "firebase",
        issuer = "https://securetoken.google.com/filler-ea8ae",
        jwksUri = "https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com")
    }
    // [END_EXCLUDE]
    )
// [END echo_api_annotation]
public class MediaMetadata {

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
        Date timestamp = new Date();

        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

        Entity uploadedMedia = new Entity("UploadedMedia");
        uploadedMedia.setIndexedProperty("userId", userId);
        uploadedMedia.setUnindexedProperty("downloadUrl", downloadUrl);
        uploadedMedia.setIndexedProperty("privacy", privacy);
        uploadedMedia.setUnindexedProperty("comment", comment);
        uploadedMedia.setIndexedProperty("timestamp", timestamp);

        datastoreService.put(uploadedMedia);

        Message returnMessage = new Message();
        returnMessage.setMessage(downloadUrl);
        return returnMessage;
    }
    // [END record_upload]

    // [START my_images]
    @ApiMethod(name = "my_images",
            path = "my_images",
            apiKeyRequired = AnnotationBoolean.TRUE)
    public MyImagesResult myImages(@Named("user_id") @Nullable String userId) {
        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

        Query.Filter propertyFilter =
        new Query.FilterPredicate("userId", Query.FilterOperator.EQUAL, userId);
        Query photoQuery = new Query("UploadedMedia").setFilter(propertyFilter);

        List<Entity> results = datastoreService.prepare(photoQuery).asList(FetchOptions.Builder.withDefaults());

        List<String> urls = new ArrayList<>();
        for (Entity result : results) {
        urls.add((String) result.getProperty("downloadUrl"));
        }

        MyImagesResult myImages = new MyImagesResult();
        myImages.setUrls(urls);

        return myImages;
    }
    // [END my_images]

    // [START date_images]
    @ApiMethod(name = "date_images",
            path = "date_images",
            apiKeyRequired = AnnotationBoolean.TRUE)
    public MyImagesResult dateImages(@Named("timestamp") @Nullable String timestamp) throws InvalidParameterException {

        Logger logger = Logger.getLogger(this.getClass().getName());

        long timestampLong = Long.valueOf(timestamp);
        try {
            timestampLong = Long.valueOf(timestamp);
        } catch (NumberFormatException e) {
            throw new InvalidParameterException();
        }

        logger.severe("timestampLong: " + timestampLong);

        long timestampStart = timestampLong - (24 * 60 * 60 * 1000); // timestamp minus (24 hrs in milliseconds)
        long timestampEnd = timestampLong + (5 * 60 * 1000);  // timestamp plus (5 minutes in milliseconds) just to be safe
        // Internet connection won't be that slow but it doesn't hurt
        // to make the end time in the future
        Date dateStart = new Date(timestampStart);
        Date dateEnd = new Date(timestampEnd);
        logger.severe("timestampStart: " + timestampStart);
        logger.severe("timestampEnd: " + timestampEnd);

        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

        Query.Filter startFilter = new Query.FilterPredicate("timestamp", Query.FilterOperator.GREATER_THAN_OR_EQUAL, dateStart);
        Query.Filter endFilter = new Query.FilterPredicate("timestamp", Query.FilterOperator.LESS_THAN, dateEnd);

        // Use CompositeFilter to combine multiple filters
        Query.CompositeFilter dateRangeFilter =
        Query.CompositeFilterOperator.and(startFilter, endFilter);

        Query photoQuery = new Query("UploadedMedia").setFilter(dateRangeFilter);
        photoQuery.addSort("timestamp", Query.SortDirection.DESCENDING);
        Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "photoQuery: " + photoQuery.toString());

        List<Entity> results = datastoreService.prepare(photoQuery).asList(FetchOptions.Builder.withDefaults());

        List<String> urls = new ArrayList<>();
        for (Entity result : results) {
            urls.add((String) result.getProperty("downloadUrl"));
        }

        MyImagesResult myImages = new MyImagesResult();
        myImages.setUrls(urls);

        return myImages;
    }
    // [END date_images]

    /* Keeping this here so that I have an example of firebase endpoint authentication for the future */
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
        issuerAudiences = {@ApiIssuerAudience(name = "firebase", audiences = {"filler-ea8ae"})}
    )
    public com.caitoo0o.filler.Email getUserEmailFirebase(User user) throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("Invalid credentials");
        }

        com.caitoo0o.filler.Email response = new com.caitoo0o.filler.Email();
        response.setEmail(user.getEmail());
        return response;
    }
    // [END firebase_auth]
}
