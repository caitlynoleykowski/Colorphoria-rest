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
import com.google.api.server.spi.config.*;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.datastore.*;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** The MediaMetadata API which Endpoints will be exposing. */
// [START echo_api_annotation]
@Api(
    name = "usermetadata",
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
public class UserMetadata {

    // [START record_upload]
    @ApiMethod(name = "record_new_user",
            path = "record_new_user",
            apiKeyRequired = AnnotationBoolean.TRUE)
    public Message record_user_metadata(
                                @Named("user_id") @Nullable String userId,
                                @Named("email") @Nullable String email,
                                @Named("full name") @Nullable String fullName,
                                @Named("username") @Nullable String username,
                                @Named("age") @Nullable String age
                                ) {
        Date date = new Date();

        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

        Entity userMetadata = new Entity("UserMetadata");
        userMetadata.setIndexedProperty("userId", userId);
        userMetadata.setUnindexedProperty("email", email);
        userMetadata.setIndexedProperty("fullName", fullName);
        userMetadata.setUnindexedProperty("username", username);
        userMetadata.setIndexedProperty("ageOnSignupDate", age);
        userMetadata.setIndexedProperty("signupDate", date);

        datastoreService.put(userMetadata);

        Message returnMessage = new Message();
        returnMessage.setMessage(""); // TODO: Don't return a message?
        return returnMessage;
    }
    // [END record_upload]

    // [START my_images]
    @ApiMethod(name = "user_metadata",
            path = "user_metadata",
            apiKeyRequired = AnnotationBoolean.TRUE)
    public UserMetadataResult userMetadata(@Named("user_id") @Nullable String userId) {
        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

        Query.Filter propertyFilter =
        new Query.FilterPredicate("userId", Query.FilterOperator.EQUAL, userId);
        Query userQuery = new Query("UserMetadata").setFilter(propertyFilter);

        List<Entity> results = datastoreService.prepare(userQuery).asList(FetchOptions.Builder.withDefaults());

        if (results.size() > 1) {
            Logger.getLogger(this.getClass().getName())
                    .severe("User has been registered multiple times. userId: "+ userId);
        }

        UserMetadataResult userMetadata = null;
        if (results.size() > 0) {
            Entity result = results.get(0);
            String userIdProperty = (String) result.getProperty("userId");
            String email = (String) result.getProperty("email");
            String fullName = (String) result.getProperty("fullName");
            String username = (String) result.getProperty("username");
            String ageOnSignupDate = (String) result.getProperty("ageOnSignupDate");
            String signupDate = (String) result.getProperty("signupDate");

            userMetadata = new UserMetadataResult(userIdProperty, email, fullName, username, ageOnSignupDate, signupDate);
        }

        // TODO: Return userMetadata inside another object which states if user is not present
        return userMetadata;
    }
    // [END my_images]
}
