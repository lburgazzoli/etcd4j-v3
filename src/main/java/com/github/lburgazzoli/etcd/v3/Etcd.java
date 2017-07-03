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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.lburgazzoli.etcd.v3.request.GetRequest;
import com.github.lburgazzoli.etcd.v3.request.PutRequest;
import com.github.lburgazzoli.etcd.v3.resolver.NameResolverFactory;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.NameResolver;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Optional.ofNullable;

public class Etcd implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Etcd.class);

    private String resolver;
    private Set<String> endpoints;
    private SslContext sslContext;
    private NameResolver.Factory nameResolverFactory;

    private ManagedChannel managedChannel;

    /**
     * Private ctor
     */
    private Etcd() {
    }

    /**
     * Close and release resources
     */
    @Override
    public void close() throws Exception {
        if (managedChannel == null) {
            managedChannel.shutdown();
        }
    }

    // **********************************
    // Operation
    // **********************************

    public PutRequest put(String key, String value) {
        return new PutRequest(
            managedChannel(),
            ByteString.copyFrom(key.getBytes()),
            ByteString.copyFrom(value.getBytes())
        );
    }

    public GetRequest get(String key) {
        return new GetRequest(
            managedChannel(),
            ByteString.copyFrom(key.getBytes())
        );
    }

    // **********************************
    //
    // **********************************

    public static Builder builder() {
        return new Builder();
    }

    // **********************************
    //
    // **********************************

    private synchronized ManagedChannel managedChannel() {
        if (managedChannel == null) {
            NettyChannelBuilder builder = NettyChannelBuilder.forTarget(this.resolver)
                .channelType(NioSocketChannel.class);

            ofNullable(nameResolverFactory).ifPresent(builder::nameResolverFactory);
            ofNullable(sslContext).ifPresent(builder::sslContext);

            if (sslContext == null) {
                builder.usePlaintext(true);
            }

            managedChannel = builder.build();
        }

        return managedChannel;
    }

    // **********************************
    // Builder
    // **********************************

    /**
     * Builder for {@link Etcd} client objects.
     */
    public static class Builder {
        private Set<String> endpoints;
        private String resolver;
        private SslContext sslContext;
        private NameResolver.Factory nameResolverFactory;

        private Builder() {
        }

        public Set<String> endpoints() {
            if (endpoints == null) {
                return Collections.emptySet();
            }

            return endpoints;
        }

        public Builder endpoints(String... endpoints) {
            return endpoints(
                Arrays.stream(endpoints)
                    .flatMap(endpoint -> Arrays.stream(endpoint.split(",")))
                    .collect(Collectors.toList())
            );
        }

        public Builder endpoints(Collection<String> endpoints) {
            if (this.endpoints == null) {
                this.endpoints = new HashSet<>();
            }

            endpoints.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(this.endpoints::add);

            return this;
        }

        public Builder resolver(String resolver) {
            this.resolver = resolver;
            return this;
        }

        public String resolver() {
            return resolver;
        }

        public Builder sslContext(SslContext sslContext) {
            this.sslContext = sslContext;
            return this;
        }

        public SslContext sslContext() {
            return sslContext;
        }

        public NameResolver.Factory nameResolverFactory() {
            return nameResolverFactory;
        }

        public Builder setNameResolverFactory(NameResolver.Factory nameResolverFactory) {
            this.nameResolverFactory = nameResolverFactory;
            return this;
        }

        /**
         * Constructs a new {@link Etcd} client.
         *
         * @return A new Etcd client.
         */
        public Etcd build() {
            Etcd etcd = new Etcd();
            etcd.resolver = ofNullable(resolver).orElse(EtcdConstants.DEFAULT_RESOLVER);
            etcd.endpoints = ofNullable(endpoints).orElseGet(Collections::emptySet);
            etcd.sslContext = ofNullable(sslContext).orElse(null);
            etcd.nameResolverFactory = ofNullable(nameResolverFactory).orElseGet(() -> new NameResolverFactory(etcd.endpoints));

            return etcd;
        }
    }
}
