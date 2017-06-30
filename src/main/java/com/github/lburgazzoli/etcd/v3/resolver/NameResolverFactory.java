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

import java.net.URI;
import java.util.Set;
import javax.annotation.Nullable;

import com.github.lburgazzoli.etcd.v3.EtcdConstants;
import io.grpc.Attributes;
import io.grpc.NameResolver;
import io.grpc.internal.DnsNameResolverProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NameResolverFactory extends NameResolver.Factory {
    private static final Logger LOGGER = LoggerFactory.getLogger(NameResolverFactory.class);

    private final Set<String> endpoints;

    public NameResolverFactory(Set<String> endpoints) {
        this.endpoints = endpoints;
    }

    @Nullable
    @Override
    public NameResolver newNameResolver(URI targetUri, Attributes params) {
        if (EtcdConstants.STATIC_RESOLVER.equals(targetUri.getPath())) {
            return new NameResolvers.Static(endpoints);
        }
        if (EtcdConstants.DNS_RESOLVER.equals(targetUri.getPath())) {
            return new DnsNameResolverProvider().newNameResolver(targetUri, params);
        }
        if (EtcdConstants.DNS_SRV_RESOLVER.equals(targetUri.getPath())) {
            return new NameResolvers.DnsSrv(endpoints);
        }

        throw new IllegalArgumentException("Unknown resolver: " + targetUri);
    }

    @Override
    public String getDefaultScheme() {
        return EtcdConstants.STATIC_RESOLVER;
    }
}
