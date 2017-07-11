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

import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestSupport {
    protected static final Logger LOGGER = LoggerFactory.getLogger(TestSupport.class);
    protected static final String ETCD_IP = System.getProperty("test.etcd.ip", "127.0.0.1");
    protected static final int ETCD_PORT = Integer.getInteger("test.etcd.port", 2379);
    protected static final String ETCD_URI = String.format("%s:%d", ETCD_IP, ETCD_PORT);

    @Before
    public void setUp() {
        LOGGER.info("ip: {}, port: {}, url: {}", ETCD_IP, ETCD_PORT, ETCD_URI);
    }
}
