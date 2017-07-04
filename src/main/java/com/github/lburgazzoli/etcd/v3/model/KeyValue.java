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
package com.github.lburgazzoli.etcd.v3.model;

/**
 * Etcd key value pair.
 */
public class KeyValue {
    private com.github.lburgazzoli.etcd.v3.api.KeyValue kv;

    public KeyValue(com.github.lburgazzoli.etcd.v3.api.KeyValue kv) {
        this.kv = kv;
    }

    public CharSequence getKey() {
        return new String(kv.getKey().toByteArray());
    }

    public CharSequence getValue() {
        return new String(kv.getValue().toByteArray());
    }

    public long getCreateRevision() {
        return kv.getCreateRevision();
    }

    public long getModRevision() {
        return kv.getModRevision();
    }

    public long getVersion() {
        return kv.getVersion();
    }

    public long getLease() {
        return kv.getLease();
    }
}
