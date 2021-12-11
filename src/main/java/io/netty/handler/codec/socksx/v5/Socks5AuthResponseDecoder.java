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
package io.netty.handler.codec.socksx.v5;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.codec.socksx.v5.Socks5AuthResponseDecoder.State;

import java.util.List;

/**
 * Decodes {@link ByteBuf}s into {@link Socks5AuthResponse}.
 * Before returning SocksResponse decoder removes itself from pipeline.
 */
public class Socks5AuthResponseDecoder extends ReplayingDecoder<State> {
    private Socks5SubnegotiationVersion version;
    private Socks5AuthStatus authStatus;
    private Socks5Response msg = UnknownSocks5Response.INSTANCE;

    public Socks5AuthResponseDecoder() {
        super(State.CHECK_PROTOCOL_VERSION);
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> out)
            throws Exception {
        switch (state()) {
            case CHECK_PROTOCOL_VERSION: {
                version = Socks5SubnegotiationVersion.valueOf(byteBuf.readByte());
                if (version != Socks5SubnegotiationVersion.AUTH_PASSWORD) {
                    break;
                }
                checkpoint(State.READ_AUTH_RESPONSE);
            }
            case READ_AUTH_RESPONSE: {
                authStatus = Socks5AuthStatus.valueOf(byteBuf.readByte());
                msg = new Socks5AuthResponse(authStatus);
            }
        }
        channelHandlerContext.pipeline().remove(this);
        out.add(msg);
    }

    enum State {
        CHECK_PROTOCOL_VERSION,
        READ_AUTH_RESPONSE
    }
}
