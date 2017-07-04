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
package com.github.lburgazzoli.etcd.v3.util;

import java.util.function.Function;
import java.util.function.Supplier;


public final class Suppliers {

    private Suppliers() {
    }

    public static <T> Supplier<T> memorizing(Supplier<T> delegate) {
        return new MemoizingSupplier<>(delegate);
    }

    public static <T, R> Supplier<R> memorizing(T bindValue, Function<T, R> delegate) {
        return new MemoizingFunction<>(bindValue, delegate);
    }

    // ******************************************
    // Helpers
    // ******************************************

    private static class MemoizingFunction<T, R> implements Supplier<R> {
        final Function<T, R> delegate;
        final T bindValue;

        transient volatile boolean initialized;
        transient R value;

        MemoizingFunction(T bindValue, Function<T, R> delegate) {
            this.bindValue = bindValue;
            this.delegate = delegate;
        }

        @Override
        public R get() {
            // A 2-field variant of Double Checked Locking.
            if (!initialized) {
                synchronized (this) {
                    if (!initialized) {
                        R result = delegate.apply(bindValue);
                        value = result;
                        initialized = true;
                        return result;
                    }
                }
            }
            return value;
        }
    }

    private static class MemoizingSupplier<T> implements Supplier<T> {
        final Supplier<T> delegate;
        transient volatile boolean initialized;
        transient T value;

        MemoizingSupplier(Supplier<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public T get() {
            // A 2-field variant of Double Checked Locking.
            if (!initialized) {
                synchronized (this) {
                    if (!initialized) {
                        T t = delegate.get();
                        value = t;
                        initialized = true;
                        return t;
                    }
                }
            }
            return value;
        }
    }
}
