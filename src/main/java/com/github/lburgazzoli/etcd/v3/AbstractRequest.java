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

import io.grpc.stub.AbstractStub;


abstract class AbstractRequest<S extends AbstractStub<S>, R extends Response> implements Request<R>  {
    private final Stub<S> stub;

    protected AbstractRequest(Stub<S> stub) {
        this.stub = stub;
    }

    @Override
    public CompletableFuture<R> send() {
        return stub.execute(this::execute);
    }

    protected abstract void execute(S stub, CompletableFuture<R> future);
}
