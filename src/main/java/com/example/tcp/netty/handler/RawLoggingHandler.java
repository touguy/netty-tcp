package com.example.tcp.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RawLoggingHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf) msg;
            int len = buf.readableBytes();
            byte[] arr = new byte[len];
            buf.getBytes(buf.readerIndex(), arr);

            // Hex representation
            StringBuilder hex = new StringBuilder();
            for (byte b : arr) {
                hex.append(String.format("%02X ", b));
            }

            // Try to decode as UTF-8 (may produce replacement chars)
            String asUtf8 = new String(arr, java.nio.charset.StandardCharsets.UTF_8);

            log.info("Raw bytes from {}: len={} hex=[{}] utf8=[{}]", ctx.channel().remoteAddress(), len,
                    hex.toString().trim(), asUtf8);
        }
        super.channelRead(ctx, msg);
    }
}
