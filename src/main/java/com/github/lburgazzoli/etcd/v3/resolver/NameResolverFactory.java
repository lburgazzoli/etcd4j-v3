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
package com.github.lburgazzoli.etcd.v3.resolver;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.naming.NamingEnumeration;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import com.github.lburgazzoli.etcd.v3.Etcd;
import com.github.lburgazzoli.etcd.v3.EtcdConstants;
import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.internal.DnsNameResolverProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NameResolverFactory extends NameResolver.Factory {
    private static final Logger LOGGER = LoggerFactory.getLogger(NameResolverFactory.class);

    private final Etcd.Configuration configuration;

    public NameResolverFactory(Etcd.Configuration configuration) {
        this.configuration = configuration;
    }

    @Nullable
    @Override
    public NameResolver newNameResolver(URI targetUri, Attributes params) {
        if (EtcdConstants.STATIC_RESOLVER.equals(targetUri.getPath())) {
            return new StaticNameResolver(this.configuration.getEndpoints());
        }
        if (EtcdConstants.DNS_RESOLVER.equals(targetUri.getPath())) {
            return new DnsNameResolverProvider().newNameResolver(targetUri, params);
        }
        if (EtcdConstants.DNS_SRV_RESOLVER.equals(targetUri.getPath())) {
            return new DnsNameResolverProvider().newNameResolver(targetUri, params);
        }

        throw new IllegalArgumentException("Unknown resolver: " + targetUri);
    }

    @Override
    public String getDefaultScheme() {
        return EtcdConstants.STATIC_RESOLVER;
    }

    // **************************************
    // Resolvers
    // **************************************

    private static class StaticNameResolver extends NameResolver {
        private final List<SocketAddress> addresses;

        private StaticNameResolver(Set<String> endpoints) {
            this.addresses = endpoints.stream()
                .map(endpoint ->  {
                    String[] items = endpoint.split(":");
                    if (items.length == 1) {
                        return new InetSocketAddress(items[0], EtcdConstants.DEFAULT_PORT);
                    } else if (items.length == 2) {
                        return new InetSocketAddress(items[0], Integer.parseInt(items[1]));
                    } else {
                        throw new IllegalArgumentException("Unable to parse endpoint " + endpoint);
                    }
                })
                .collect(Collectors.toList());
        }

        @Override
        public String getServiceAuthority() {
            return URI.create("//" + EtcdConstants.STATIC_RESOLVER).getAuthority();
        }

        @Override
        public void start(Listener listener) {
            listener.onAddresses(Collections.singletonList(
                new EquivalentAddressGroup(addresses)),
                Attributes.EMPTY
            );
        }

        @Override
        public void shutdown() {
        }
    }

    private static class DnsSrvNameResolver extends NameResolver {
        private static final Logger LOGGER;
        private static final String[] ATTRIBUTE_IDS;
        private static final Hashtable<String, String> ENV;

        static {
            LOGGER = LoggerFactory.getLogger(DnsSrvNameResolver.class);
            ATTRIBUTE_IDS = new String[]{"SRV"};

            ENV = new Hashtable<>();
            ENV.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            ENV.put("java.naming.provider.url", "dns:");
        }

        private final Set<String> endpoints;

        private DnsSrvNameResolver(Set<String> endpoints) {
            this.endpoints = endpoints;
        }

        @Override
        public String getServiceAuthority() {
            return URI.create("//" + EtcdConstants.DNS_SRV_RESOLVER).getAuthority();
        }

        @Override
        public void start(Listener listener) {
            try {
                List<SocketAddress> addresses = new LinkedList<>();

                for (String endpoint :  endpoints) {
                    DirContext ctx = new InitialDirContext(ENV);
                    NamingEnumeration<?> resolved = ctx.getAttributes(endpoint, ATTRIBUTE_IDS).get("srv").getAll();

                    while (resolved.hasMore()) {
                        String record = (String) resolved.next();
                        String[] split = record.split(" ");

                        addresses.add(new InetSocketAddress(split[3].trim(), Integer.parseInt(split[2].trim())));
                    }
                }

                listener.onAddresses(Collections.singletonList(
                    new EquivalentAddressGroup(addresses)),
                    Attributes.EMPTY
                );

            } catch (Exception e) {
                LOGGER.warn("", e);
            }
        }

        @Override
        public void shutdown() {
        }
    }
}
