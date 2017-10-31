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
    name = "mobile",
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
            httpMethod = ApiMethod.HttpMethod.POST,
            apiKeyRequired = AnnotationBoolean.TRUE,
            authenticators = {EspAuthenticator.class},
            issuerAudiences = {@ApiIssuerAudience(name = "firebase", audiences = {"filler-ea8ae"})}
    )
    public Message recordUpload(
                                User user,
                                @Named("download_url") @Nullable String downloadUrl,
                                @Named("filename") @Nullable String filename,
                                @Named("privacy") @Nullable String privacy,
                                @Named("comment") @Nullable String comment
                                ) throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("Invalid credentials");
        }

        String filepath = "gs://" + "filler-ea8ae.appspot.com" + "/photos/" + filename;
        boolean adultContent = ImageUtils.detectSafeSearchGcs(filepath);

        String message = "";
        if (adultContent) {
            message = "adult_content";
        } else {
            Date timestamp = new Date();

            DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

            Entity uploadedMedia = new Entity("UploadedMedia");
            uploadedMedia.setIndexedProperty("userId", user.getId());
            uploadedMedia.setUnindexedProperty("downloadUrl", downloadUrl);
            uploadedMedia.setUnindexedProperty("filename", filename);
            uploadedMedia.setIndexedProperty("privacy", privacy);
            uploadedMedia.setUnindexedProperty("comment", comment);
            uploadedMedia.setIndexedProperty("timestamp", timestamp);

            datastoreService.put(uploadedMedia);
        }

        Message returnMessage = new Message();
        returnMessage.setMessage(message);
        return returnMessage;
    }
    // [END record_upload]

    // [START my_images]
    @ApiMethod(name = "my_images",
            path = "my_images",
            httpMethod = ApiMethod.HttpMethod.POST,
            apiKeyRequired = AnnotationBoolean.TRUE,
            authenticators = {EspAuthenticator.class},
            issuerAudiences = {@ApiIssuerAudience(name = "firebase", audiences = {"filler-ea8ae"})}
    )
    public MyImagesResult myImages(User user) throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("Invalid credentials");
        }

        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

        Query.Filter propertyFilter =
        new Query.FilterPredicate("userId", Query.FilterOperator.EQUAL, user.getId());
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
            httpMethod = ApiMethod.HttpMethod.POST,
            apiKeyRequired = AnnotationBoolean.TRUE,
            authenticators = {EspAuthenticator.class},
            issuerAudiences = {@ApiIssuerAudience(name = "firebase", audiences = {"filler-ea8ae"})}
    )
    public MyImagesResult dateImages(User user, @Named("timestamp") @Nullable String timestamp)
            throws InvalidParameterException, UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("Invalid credentials");
        }
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

    // [START firebase_auth]
    @ApiMethod(
        path = "firebase_user",
        httpMethod = ApiMethod.HttpMethod.GET,
        authenticators = {EspAuthenticator.class},
        issuerAudiences = {@ApiIssuerAudience(name = "firebase", audiences = {"filler-ea8ae"})}
    )
    public com.caitoo0o.filler.Email getUserEmailFirebase(User user) throws UnauthorizedException, Exception {
        if (user == null) {
            throw new UnauthorizedException("Invalid credentials");
        }

        com.caitoo0o.filler.Email response = new com.caitoo0o.filler.Email();
        response.setEmail(user.getEmail());
        return response;
    }

    @ApiMethod(
            path = "firebase_user2",
            httpMethod = ApiMethod.HttpMethod.POST,
            apiKeyRequired = AnnotationBoolean.TRUE,
            authenticators = {EspAuthenticator.class},
            issuerAudiences = {@ApiIssuerAudience(name = "firebase", audiences = {"filler-ea8ae"})}
    )
    public com.caitoo0o.filler.Email getUserEmailFirebase2(User user) throws UnauthorizedException, Exception {
        if (user == null) {
            throw new UnauthorizedException("Invalid credentials");
        }

        com.caitoo0o.filler.Email response = new com.caitoo0o.filler.Email();
        response.setEmail(user.getEmail());
        return response;
    }
    // [END firebase_auth]
}
