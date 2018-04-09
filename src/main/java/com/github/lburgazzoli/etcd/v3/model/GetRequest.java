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
package com.github.lburgazzoli.etcd.v3.model;

import com.github.lburgazzoli.etcd.v3.api.KVGrpc;
import com.github.lburgazzoli.etcd.v3.util.Rx;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.reactivex.Single;

public class GetRequest implements Request<GetResponse> {
    private final ManagedChannel channel;
    private final KVGrpc.KVFutureStub stub;
    private final ByteString key;

    public GetRequest(ManagedChannel channel, ByteString key) {
        this.channel = channel;
        this.stub = KVGrpc.newFutureStub(this.channel);
        this.key = key;
    }

    @Override
    public Single<GetResponse> send() {
        com.github.lburgazzoli.etcd.v3.api.RangeRequest request =
            com.github.lburgazzoli.etcd.v3.api.RangeRequest.newBuilder()
                .setKey(key)
                .build();

        return Rx.toSingle(() -> stub.range(request)).map(GetResponse::new);
    }
}
