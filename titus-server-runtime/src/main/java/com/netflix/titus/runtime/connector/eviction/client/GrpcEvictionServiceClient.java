/*
 * Copyright 2018 Netflix, Inc.
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

package com.netflix.titus.runtime.connector.eviction.client;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.netflix.titus.api.eviction.model.EvictionQuota;
import com.netflix.titus.api.eviction.model.event.EvictionEvent;
import com.netflix.titus.api.model.reference.Reference;
import com.netflix.titus.grpc.protogen.EvictionServiceEvent;
import com.netflix.titus.grpc.protogen.EvictionServiceGrpc.EvictionServiceStub;
import com.netflix.titus.grpc.protogen.ObserverEventRequest;
import com.netflix.titus.grpc.protogen.TaskTerminateRequest;
import com.netflix.titus.runtime.connector.GrpcClientConfiguration;
import com.netflix.titus.runtime.connector.eviction.EvictionServiceClient;
import com.netflix.titus.runtime.endpoint.common.grpc.GrpcUtil;
import com.netflix.titus.runtime.endpoint.metadata.CallMetadataResolver;
import com.netflix.titus.runtime.eviction.endpoint.grpc.GrpcEvictionModelConverters;
import io.grpc.stub.StreamObserver;
import rx.Completable;
import rx.Observable;

import static com.netflix.titus.runtime.endpoint.common.grpc.GrpcUtil.createSimpleClientResponseObserver;
import static com.netflix.titus.runtime.endpoint.common.grpc.GrpcUtil.createWrappedStub;
import static com.netflix.titus.runtime.eviction.endpoint.grpc.GrpcEvictionModelConverters.toGrpcReference;

@Singleton
public class GrpcEvictionServiceClient implements EvictionServiceClient {

    private final GrpcClientConfiguration configuration;
    private final EvictionServiceStub client;
    private final CallMetadataResolver callMetadataResolver;

    @Inject
    public GrpcEvictionServiceClient(GrpcClientConfiguration configuration,
                                     EvictionServiceStub client,
                                     CallMetadataResolver callMetadataResolver) {
        this.configuration = configuration;
        this.client = client;
        this.callMetadataResolver = callMetadataResolver;
    }

    @Override
    public Observable<EvictionQuota> getEvictionQuota(Reference reference) {
        return GrpcUtil.<com.netflix.titus.grpc.protogen.EvictionQuota>createRequestObservable(emitter -> {
                    StreamObserver<com.netflix.titus.grpc.protogen.EvictionQuota> streamObserver = createSimpleClientResponseObserver(emitter);
                    createWrappedStub(client, callMetadataResolver, configuration.getRequestTimeout()).getEvictionQuota(toGrpcReference(reference), streamObserver);
                },
                configuration.getRequestTimeout()
        ).map(GrpcEvictionModelConverters::toCoreEvictionQuota);
    }

    @Override
    public Completable terminateTask(String taskId, String reason) {
        return GrpcUtil.<com.netflix.titus.grpc.protogen.TaskTerminateResponse>createRequestObservable(emitter -> {
                    StreamObserver<com.netflix.titus.grpc.protogen.TaskTerminateResponse> streamObserver = createSimpleClientResponseObserver(emitter);
                    TaskTerminateRequest request = TaskTerminateRequest.newBuilder()
                            .setTaskId(taskId)
                            .setReason(reason)
                            .build();
                    createWrappedStub(client, callMetadataResolver, configuration.getRequestTimeout()).terminateTask(request, streamObserver);
                },
                configuration.getRequestTimeout()
        ).flatMap(response -> {
            if (!response.getAllowed()) {
                // TODO Better error handling
                return Observable.error(new IllegalStateException(response.getReasonCode() + ": " + response.getReasonMessage()));
            }
            return Observable.empty();
        }).toCompletable();
    }

    @Override
    public Observable<EvictionEvent> observeEvents(boolean includeSnapshot) {
        return GrpcUtil.<EvictionServiceEvent>createRequestObservable(emitter -> {
            StreamObserver<EvictionServiceEvent> streamObserver = createSimpleClientResponseObserver(emitter);
            ObserverEventRequest request = ObserverEventRequest.newBuilder()
                    .setIncludeSnapshot(includeSnapshot)
                    .build();
            createWrappedStub(client, callMetadataResolver).observeEvents(request, streamObserver);
        }).map(GrpcEvictionModelConverters::toCoreEvent);
    }
}
