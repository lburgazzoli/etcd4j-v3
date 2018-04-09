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
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

import com.github.lburgazzoli.etcd.v3.api.AuthGrpc;
import com.github.lburgazzoli.etcd.v3.api.AuthenticateRequest;
import com.github.lburgazzoli.etcd.v3.model.GetRequest;
import com.github.lburgazzoli.etcd.v3.model.PutRequest;
import com.github.lburgazzoli.etcd.v3.resolver.NameResolverFactory;
import com.google.common.base.Strings;
import com.google.protobuf.ByteString;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.LoadBalancer;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.NameResolver;
import io.grpc.PickFirstBalancerFactory;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Etcd implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Etcd.class);
    private final Metadata.Key<String> TOKEN = Metadata.Key.of("token", Metadata.ASCII_STRING_MARSHALLER);

    private String user;
    private String password;
    private String resolver;
    private Set<String> endpoints;
    private SslContext sslContext;
    private NameResolver.Factory nameResolverFactory;
    private LoadBalancer.Factory loadBalancerFactory;
    private ManagedChannel managedChannel;
    private long tokenExpirationTime;
    private TimeUnit tokenExpirationTimeUnit;
    private long tokenExpirationJitter;
    private TimeUnit tokenExpirationJitterUnit;

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
                .channelType(NioSocketChannel.class)
                .intercept(new Interceptor());

            if (nameResolverFactory != null) {
                builder.nameResolverFactory(nameResolverFactory);
            }
            if (loadBalancerFactory != null) {
                builder.loadBalancerFactory(loadBalancerFactory);
            }
            if (sslContext != null) {
                builder.sslContext(sslContext);
            }
            if (sslContext == null) {
                builder.negotiationType(NegotiationType.PLAINTEXT);
            }

            managedChannel = builder.build();
        }

        return managedChannel;
    }


    // **********************************
    // Token
    // **********************************

    private class Token {
        private final long timeout;
        private final long jitter;
        private long nextTimeout;
        private String token;

        Token() {
            this.nextTimeout = 0;
            this.timeout = tokenExpirationTimeUnit.toMillis(tokenExpirationTime);
            this.jitter = tokenExpirationJitterUnit.toMillis(tokenExpirationJitter);
        }

        public synchronized void refresh(Channel channel, Consumer<String> consumer) {
            if (Strings.isNullOrEmpty(user) || Strings.isNullOrEmpty(password)) {
                return;
            }

            try {
                long currentTime = System.currentTimeMillis();

                if (nextTimeout < currentTime) {
                    LOGGER.debug("Refresh token");

                    token = AuthGrpc.newFutureStub(channel).authenticate(
                        AuthenticateRequest.newBuilder()
                            .setName(user)
                            .setPassword(password)
                            .build())
                        .get()
                        .getToken();

                    nextTimeout = currentTime + timeout - jitter;
                }

                if (token != null) {
                    consumer.accept(token);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public void touch() {
            nextTimeout = System.currentTimeMillis() + timeout - jitter;
        }
    }

    private class Interceptor implements ClientInterceptor {
        private final Token token;

        Interceptor() {
            token = new Token();
        }

        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
            return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
                @Override
                public void start(Listener<RespT> responseListener, Metadata headers) {
                    token.refresh(next, t -> headers.put(TOKEN, t));

                    super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {
                        @Override
                        public void onMessage(RespT message) {
                            token.touch();
                            super.onMessage(message);
                        }
                    },
                    headers);
                }
            };
        }
    }


    // **********************************
    // Builder
    // **********************************

    /**
     * Builder for {@link Etcd} client objects.
     */
    public static class Builder {
        private String user;
        private String password;
        private Set<String> endpoints;
        private String resolver;
        private SslContext sslContext;
        private NameResolver.Factory nameResolverFactory;
        private LoadBalancer.Factory loadBalancerFactory;
        private Long tokenExpirationTime;
        private TimeUnit tokenExpirationTimeUnit;
        private Long tokenExpirationJitter;
        private TimeUnit tokenExpirationJitterUnit;

        private Builder() {
        }

        public String user() {
            return user;
        }

        public Builder user(String user) {
            this.user = user;
            return this;
        }

        public String password() {
            return password;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder tokenExpiration(Long tokenExpirationTime, TimeUnit tokenExpirationTimeUnit) {
            this.tokenExpirationTime = tokenExpirationTime;
            this.tokenExpirationTimeUnit = tokenExpirationTimeUnit;
            return this;
        }

        public Long tokenExpirationTime() {
            return tokenExpirationTime;
        }

        public Builder tokenExpirationTime(Long tokenExpirationTime) {
            this.tokenExpirationTime = tokenExpirationTime;
            return this;
        }

        public TimeUnit tokenExpirationTimeUnit() {
            return tokenExpirationTimeUnit;
        }

        public Builder tokenExpirationTimeUnit(TimeUnit tokenExpirationTimeUnit) {
            this.tokenExpirationTimeUnit = tokenExpirationTimeUnit;
            return this;
        }

        public Builder tokenExpirationJitter(Long tokenExpirationJitter, TimeUnit tokenExpirationJitterUnit) {
            this.tokenExpirationJitter = tokenExpirationJitter;
            this.tokenExpirationJitterUnit = tokenExpirationJitterUnit;
            return this;
        }

        public Long tokenExpirationJitter() {
            return tokenExpirationJitter;
        }

        public Builder tokenExpirationJitter(Long tokenExpirationJitter) {
            this.tokenExpirationJitter = tokenExpirationJitter;
            return this;
        }

        public TimeUnit tokenExpirationJitterUnit() {
            return tokenExpirationJitterUnit;
        }

        public Builder tokenExpirationJitterUnit(TimeUnit tokenExpirationJitterUnit) {
            this.tokenExpirationJitterUnit = tokenExpirationJitterUnit;
            return this;
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

        public Builder nameResolverFactory(NameResolver.Factory nameResolverFactory) {
            this.nameResolverFactory = nameResolverFactory;
            return this;
        }

        public LoadBalancer.Factory loadBalancerFactory() {
            return loadBalancerFactory;
        }

        public Builder loadBalancerFactory(LoadBalancer.Factory loadBalancerFactory) {
            this.loadBalancerFactory = loadBalancerFactory;
            return this;
        }

        /**
         * Constructs a new {@link Etcd} client.
         *
         * @return A new Etcd client.
         */
        public Etcd build() {
            Etcd etcd = new Etcd();
            etcd.user = user;
            etcd.password = password;
            etcd.tokenExpirationTime = ofNullable(tokenExpirationTime).orElse(5L);
            etcd.tokenExpirationTimeUnit = ofNullable(tokenExpirationTimeUnit).orElse(TimeUnit.MINUTES);
            etcd.tokenExpirationJitter = ofNullable(tokenExpirationJitter).orElse(5L);
            etcd.tokenExpirationJitterUnit = ofNullable(tokenExpirationJitterUnit).orElse(TimeUnit.SECONDS);
            etcd.endpoints = ofNullable(endpoints).orElseGet(Collections::emptySet);
            etcd.resolver = ofNullable(resolver).orElse(EtcdConstants.DEFAULT_RESOLVER);
            etcd.endpoints = ofNullable(endpoints).orElseGet(Collections::emptySet);
            etcd.sslContext = ofNullable(sslContext).orElse(null);
            etcd.nameResolverFactory = ofNullable(nameResolverFactory).orElseGet(() -> new NameResolverFactory(etcd.endpoints));
            etcd.loadBalancerFactory = ofNullable(loadBalancerFactory).orElseGet(PickFirstBalancerFactory::getInstance);

            return etcd;
        }
    }
}
