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

import com.github.lburgazzoli.etcd.v3.api.ResponseHeader;

abstract class AbstractResponse<R> implements Response {
    private final R response;
    private final ResponseHeader responseHeader;
    private final Header header;

    AbstractResponse(R response, ResponseHeader responseHeader) {
        this.response = response;
        this.responseHeader = responseHeader;

        this.header = new Header();
    }

    // *****************************************
    //
    // *****************************************

    @Override
    public final Header getHeader() {
        return header;
    }


    // *****************************************
    //
    // *****************************************

    protected final R response() {
        return this.response;
    }

    protected final ResponseHeader responseHeader() {
        return this.responseHeader;
    }

    private class Header implements Response.Header {
        @Override
        public long getClusterId() {
            return responseHeader.getClusterId();
        }

        @Override
        public long getMemberId() {
            return responseHeader.getMemberId();
        }

        @Override
        public long getRevision() {
            return responseHeader.getRevision();
        }

        @Override
        public long getRaftTerm() {
            return responseHeader.getRaftTerm();
        }
    }
}
