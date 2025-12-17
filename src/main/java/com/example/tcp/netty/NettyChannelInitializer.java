package com.example.tcp.netty;

import com.example.tcp.config.NettyProperties;
import com.example.tcp.netty.handler.BusinessLogicHandler;
import com.example.tcp.service.SessionManager;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class NettyChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final NettyProperties properties;
    private final SessionManager sessionManager;

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline()
                // 1. IdleStateHandler: Timeout/KeepAlive 감지 (Hot Deploy 설정 값 반영)
                .addLast(new IdleStateHandler(
                        properties.getReaderIdleTime(),
                        properties.getWriterIdleTime(),
                        properties.getAllIdleTime(),
                        TimeUnit.SECONDS))

                // Raw logging for incoming bytes to help debug encoding/frame issues
                .addLast(new com.example.tcp.netty.handler.RawLoggingHandler())

                // 2. Codec: 문자열 기반 (실무에서는 ByteArrayDecoder 등을 사용)
                .addLast(new StringDecoder(CharsetUtil.UTF_8))
                .addLast(new StringEncoder(CharsetUtil.UTF_8))

                // 3. Business Logic
                .addLast(new BusinessLogicHandler(sessionManager, properties));
    }
}