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

import java.util.Optional;

import io.grpc.Metadata;
import io.grpc.stub.AbstractStub;

final class EtcdUtils {
    private EtcdUtils() {
    }

    static final <T extends AbstractStub<T>> T configureStub(T stub, Optional<String> token) {
        return token.map(t -> {
                Metadata metadata = new Metadata();
                metadata.put(
                    Metadata.Key.of(EtcdConstants.AUTHENTICATION_TOKEN, Metadata.ASCII_STRING_MARSHALLER),
                    t);

                return stub.withCallCredentials(
                    (methodDescriptor, attributes, executor, applier) -> applier.apply(metadata)
                );
            }
        ).orElse(stub);
    }
}
