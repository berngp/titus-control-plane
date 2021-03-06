/*
 * Copyright 2019 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.titus.ext.aws.appscale;


import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.netflix.titus.runtime.endpoint.common.rest.Responses;
import io.swagger.annotations.Api;
import io.swagger.jaxrs.PATCH;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api(tags = "Auto scaling")
@Path("/v1/scalableTargetDimensions")
@Singleton
public class AppAutoScalingCallbackResource {
    private static final Logger logger = LoggerFactory.getLogger(AppAutoScalingCallbackResource.class);
    private final AppAutoScalingCallbackService awsGatewayCallbackService;

    @Inject
    public AppAutoScalingCallbackResource(AppAutoScalingCallbackService awsGatewayCallbackService) {
        this.awsGatewayCallbackService = awsGatewayCallbackService;
    }

    @Path("{scalableTargetDimensionId}")
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public ScalableTargetResourceInfo getScalableTargetResourceInfo(@PathParam("scalableTargetDimensionId") String jobId) {
        return Responses.fromSingleValueObservable(awsGatewayCallbackService.getScalableTargetResourceInfo(jobId));
    }

    @Path("{scalableTargetDimensionId}")
    @PATCH
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public ScalableTargetResourceInfo setScalableTargetResourceInfo(@PathParam("scalableTargetDimensionId") String jobId, ScalableTargetResourceInfo scalableTargetResourceInfo) {
        if (scalableTargetResourceInfo.getDesiredCapacity() < 0) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        return Responses.fromSingleValueObservable(awsGatewayCallbackService.setScalableTargetResourceInfo(jobId, scalableTargetResourceInfo));
    }
}
