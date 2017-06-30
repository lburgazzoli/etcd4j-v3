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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.lburgazzoli.etcd.v3.resolver.NameResolverFactory;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Etcd {
    private static final Logger LOGGER = LoggerFactory.getLogger(Etcd.class);

    private final Configuration configuration;

    /**
     * Private ctor
     */
    private Etcd(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Close and release resources
     */
    public void close() {
    }

    public static Builder builder() {
        return new Builder();
    }

    // **********************************
    // Clients
    // **********************************

    public KVClient kvClient() {
        return new KVClient(
            NettyChannelBuilder.forTarget(configuration.getResolver())
                .channelType(NioSocketChannel.class)
                .nameResolverFactory(new NameResolverFactory(configuration))
                .sslContext(configuration.sslContext)
                .usePlaintext(configuration.sslContext == null)
                .build()
        );
    }

    // **********************************
    // Configuration
    // **********************************

    public static class Configuration {
        private String resolver;
        private Set<String> endpoints;
        private SslContext sslContext;

        private Configuration() {
        }

        public Set<String> getEndpoints() {
            return endpoints;
        }

        public String getResolver() {
            return resolver;
        }

        public SslContext getSslContext() {
            return sslContext;
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

        /**
         * Constructs a new {@link Etcd} client.
         *
         * @return A new Etcd client.
         */
        public Etcd build() {
            Configuration config = new Configuration();
            config.resolver = Optional.ofNullable(resolver).orElse(EtcdConstants.DEFAULT_RESOLVER);
            config.endpoints = Optional.ofNullable(endpoints).orElseGet(Collections::emptySet);
            config.sslContext = Optional.ofNullable(sslContext).orElse(null);

            return new Etcd(config);
        }
    }
}
