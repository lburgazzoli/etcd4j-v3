/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.lburgazzoli.etcd.v3;

import java.util.concurrent.CompletableFuture;

import com.github.lburgazzoli.etcd.v3.api.KVGrpc;
import com.github.lburgazzoli.etcd.v3.api.PutRequest;
import com.github.lburgazzoli.etcd.v3.api.PutResponse;
import com.github.lburgazzoli.etcd.v3.api.RangeRequest;
import com.github.lburgazzoli.etcd.v3.api.RangeResponse;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import net.javacrumbs.futureconverter.java8guava.FutureConverter;

public final class KVClient {
    private final ManagedChannel channel;
    private final KVGrpc.KVFutureStub stub;

    KVClient(ManagedChannel channel) {
        this.channel = channel;
        this.stub = KVGrpc.newFutureStub(this.channel);
    }

    public CompletableFuture<PutResponse> put(String key, String value) {
        PutRequest request = PutRequest.newBuilder()
            .setKey(ByteString.copyFrom(key.getBytes()))
            .setValue(ByteString.copyFrom(value.getBytes()))
            .build();

        return FutureConverter.toCompletableFuture(stub.put(request));
    }

    public CompletableFuture<RangeResponse> range(String key) {
        RangeRequest request = RangeRequest.newBuilder()
            .setKey(ByteString.copyFrom(key.getBytes()))
            .build();

        return FutureConverter.toCompletableFuture(stub.range(request));
    }
}
