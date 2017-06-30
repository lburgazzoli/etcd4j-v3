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
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.github.lburgazzoli.etcd.v3.api.PutResponse;
import com.github.lburgazzoli.etcd.v3.api.RangeResponse;
import com.github.lburgazzoli.etcd.v3.impl.KV;
import com.github.lburgazzoli.etcd.v3.resolver.NameResolverFactory;
import io.grpc.ManagedChannel;
import io.grpc.NameResolver;
import io.netty.handler.ssl.SslContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Optional.ofNullable;

public class Etcd implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Etcd.class);

    private final Configuration configuration;
    private ManagedChannel managedChannel;
    private KV kv;

    /**
     * Private ctor
     */
    private Etcd(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Close and release resources
     */
    @Override
    public void close() throws Exception {
        try {
            if (kv != null) {
                kv.close();
            }
        } finally {
            if (managedChannel == null) {
                managedChannel.shutdown();
            }
        }
    }

    // **********************************
    // Operation
    // **********************************

    public CompletableFuture<PutResponse> put(String key, String value) {
        return kv().put(key.getBytes(), value.getBytes());
    }

    public CompletableFuture<RangeResponse> range(String key) {
        return kv().range(key.getBytes());
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
            managedChannel = EtcdUtils.managedChannel(configuration);
        }

        return managedChannel;
    }

    private synchronized KV kv() {
        if (kv == null) {
            kv = new KV(managedChannel());
        }

        return kv;
    }

    // **********************************
    // Configuration
    // **********************************

    public static class Configuration {
        private String resolver;
        private Set<String> endpoints;
        private SslContext sslContext;
        private boolean useSsl;
        private NameResolver.Factory nameResolverFactory;

        private Configuration() {
        }

        public Set<String> endpoints() {
            return endpoints;
        }

        public String resolver() {
            return resolver;
        }

        public SslContext sslContext() {
            return sslContext;
        }

        public boolean isUseSsl() {
            return useSsl;
        }

        public NameResolver.Factory nameResolverFactory() {
            return nameResolverFactory;
        }
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
        private Boolean useSsl;
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

        public Builder useSsl(Boolean useSsl) {
            this.useSsl = useSsl;
            return this;
        }

        public Boolean useSsl() {
            return useSsl;
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
            Configuration config = new Configuration();
            config.resolver = ofNullable(resolver).orElse(EtcdConstants.DEFAULT_RESOLVER);
            config.endpoints = ofNullable(endpoints).orElseGet(Collections::emptySet);
            config.sslContext = ofNullable(sslContext).orElse(null);
            config.useSsl = ofNullable(useSsl).orElse(Boolean.TRUE);
            config.nameResolverFactory = ofNullable(nameResolverFactory).orElseGet(() -> new NameResolverFactory(config.endpoints));

            return new Etcd(config);
        }
    }
}
