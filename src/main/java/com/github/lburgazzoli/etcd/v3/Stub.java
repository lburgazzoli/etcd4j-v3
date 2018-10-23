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
import java.util.concurrent.Executor;
import java.util.function.Function;

import com.github.lburgazzoli.etcd.v3.util.ThrowingBiConsumer;

public class Stub<S extends io.grpc.stub.AbstractStub<S>> {
    private final Executor executor;
    private final S stub;

    public Stub(S stub, Executor executor) {
        this.stub = stub;
        this.executor = executor;
    }

    public <R, E extends Exception> CompletableFuture<R> execute(ThrowingBiConsumer<S, CompletableFuture<R>, E> consumer) {
        CompletableFuture<R> future = new CompletableFuture<>();

        try {
            consumer.accept(stub, future);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return future.thenApplyAsync(Function.identity(), executor);
    }
}
