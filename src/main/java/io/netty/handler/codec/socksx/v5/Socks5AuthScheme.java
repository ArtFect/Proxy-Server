/*
 * Copyright 2013 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.netty.handler.codec.socksx.v5;

public enum Socks5AuthScheme {
    NO_AUTH((byte) 0x00),
    AUTH_GSSAPI((byte) 0x01),
    AUTH_PASSWORD((byte) 0x02),
    UNKNOWN((byte) 0xff);

    private final byte b;

    Socks5AuthScheme(byte b) {
        this.b = b;
    }

    public static Socks5AuthScheme valueOf(byte b) {
        for (Socks5AuthScheme code : values()) {
            if (code.b == b) {
                return code;
            }
        }
        return UNKNOWN;
    }

    public byte byteValue() {
        return b;
    }
}

