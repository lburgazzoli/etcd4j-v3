/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.lburgazzoli.etcd.v3;

import com.github.lburgazzoli.etcd.v3.support.EtcdClusterResource;
import io.vertx.core.net.PemKeyCertOptions;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class SslTest {
    @Rule
    public final EtcdClusterResource cluster = new EtcdClusterResource("etcd-ssl", true);

    @Test(timeout = 5000)
    public void testSimpleSllSetup() {
        String root = System.getProperty("project.path");
        Etcd etcd = Etcd.builder()
            .endpoint(cluster.cluster().getClientEndpoints().get(0))
            .clientOptionsHandler(options -> {
                options
                    .setSsl(true)
                    .setUseAlpn(true)
                    .setTrustAll(true)
                    .setPemKeyCertOptions(
                        new PemKeyCertOptions()
                            .addKeyPath(root + "/src/test/resources/ssl/cert/ca-key.pem")
                            .addCertPath(root + "/src/test/resources/ssl/cert/ca.pem")
                    );
            })
            .build();

        PutResponse put = etcd.put("key", "value").get();
        GetResponse get = etcd.get("key").get();

        Assert.assertFalse(put.hasPrevKv());
        Assert.assertFalse(get.getMore());
        Assert.assertEquals(1, get.getCount());
        Assert.assertEquals("key", get.getKvs().get(0).getKey());
        Assert.assertEquals("value", get.getKvs().get(0).getValue());
    }
}
