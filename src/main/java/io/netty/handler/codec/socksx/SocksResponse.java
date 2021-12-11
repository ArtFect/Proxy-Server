/*
 * Copyright 2012 The Netty Project
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
package io.netty.handler.codec.socksx;

import io.netty.handler.codec.socksx.v4.Socks4Response;
import io.netty.handler.codec.socksx.v5.Socks5Response;

/**
 * An abstract class that defines a SOCKS response, providing common properties for
 * {@link Socks4Response} and {@link Socks5Response}.
 */
public abstract class SocksResponse extends SocksMessage {
    protected SocksResponse(SocksProtocolVersion protocolVersion) {
        super(protocolVersion, SocksMessageType.RESPONSE);
    }
}
