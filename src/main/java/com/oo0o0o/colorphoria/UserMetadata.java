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

package com.oo0o0o.colorphoria;

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
    name = "mobile",
    version = "v1",
    namespace =
      @ApiNamespace(
        ownerDomain = "colorphoria.com",
        ownerName = "colorphoria.com",
        packagePath = ""
      ),
    // [START_EXCLUDE]
    issuers = {
      @ApiIssuer(
        name = "firebase",
        issuer = "https://securetoken.google.com/colorphoria-app",
        jwksUri = "https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com")
    }
    // [END_EXCLUDE]
    )
// [END echo_api_annotation]
public class UserMetadata {

    @ApiMethod(name = "record_new_user",
            path = "record_new_user",
            httpMethod = ApiMethod.HttpMethod.POST,
            apiKeyRequired = AnnotationBoolean.TRUE,
            authenticators = {EspAuthenticator.class},
            issuerAudiences = {@ApiIssuerAudience(name = "firebase", audiences = {"colorphoria-app"})}
    )
    public Message record_user_metadata(
                                User user,
                                @Named("email") @Nullable String email,
                                @Named("full_name") @Nullable String fullName,
                                @Named("username") @Nullable String username,
                                @Named("birthday_timestamp_str") @Nullable String birthdayTimestampStr
                                ) throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("Invalid credentials");
        }

        // Get today's date for the user's signupDate
        Date todaysDate = new Date();

        // Get the birthdayTimestampStr in Date object form
        Long birthdayTimestamp = Long.valueOf(birthdayTimestampStr);
        try {
            birthdayTimestamp = Long.valueOf(birthdayTimestampStr);
        } catch (NumberFormatException e) {
            throw new InvalidParameterException("birthdayDate must be a timestamp long sent as a string");
        }
        Date birthdayDate = new Date(birthdayTimestamp);

        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

        Entity userMetadata = new Entity("UserMetadata");
        userMetadata.setIndexedProperty("userId", user.getId());
        userMetadata.setUnindexedProperty("email", email); // TODO: Remove
        userMetadata.setUnindexedProperty("fullName", fullName);
        userMetadata.setUnindexedProperty("username", username);
        userMetadata.setUnindexedProperty("birthdayDate", birthdayDate);
        userMetadata.setUnindexedProperty("signupDate", todaysDate);

        datastoreService.put(userMetadata);

        Message returnMessage = new Message();
        returnMessage.setMessage(""); // TODO: Don't return a message?
        return returnMessage;
    }

    @ApiMethod(name = "user_recorded_status",
            path = "user_recorded_status",
            httpMethod = ApiMethod.HttpMethod.POST,
            apiKeyRequired = AnnotationBoolean.TRUE,
            authenticators = {EspAuthenticator.class},
            issuerAudiences = {@ApiIssuerAudience(name = "firebase", audiences = {"colorphoria-app"})}
    )
    public UserRecordStatus userRecordedStatus(User user) throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("Invalid credentials");
        }

        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

        Query.Filter propertyFilter =
        new Query.FilterPredicate("userId", Query.FilterOperator.EQUAL, user.getId());
        Query userQuery = new Query("UserMetadata").setFilter(propertyFilter);

        List<Entity> results = datastoreService.prepare(userQuery).asList(FetchOptions.Builder.withDefaults());

        if (results.size() > 1) {
            Logger.getLogger(this.getClass().getName())
                    .severe("User has been registered multiple times. userId: "+ user.getId());
        }

        UserRecordStatus recordedStatus = null;
        if (results.size() > 0) {
            recordedStatus = new UserRecordStatus(true);
        } else {
            recordedStatus = new UserRecordStatus(false);
        }

        return recordedStatus;
    }
}
