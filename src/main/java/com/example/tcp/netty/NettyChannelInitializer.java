package com.example.tcp.netty;

import com.example.tcp.config.NettyProperties;
import com.example.tcp.decoder.TestDecoder;
import com.example.tcp.netty.handler.BusinessLogicHandler;
import com.example.tcp.netty.handler.TestHandler;
import com.example.tcp.service.SessionManager;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class NettyChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final NettyProperties properties;
    private final SessionManager sessionManager;

    // CP949 Charset 객체 생성 (문자열 "CP949"는 Java 표준 명칭입니다)
    //private static final Charset CP949_CHARSET = Charset.forName("CP949");
    
    private final TestHandler testHandler;

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {

        // decoder는 @Sharable이 안 됨, Bean 객체 주입이 안 되고, 매번 새로운 객체 생성해야 함
        TestDecoder testDecoder = new TestDecoder();


        ch.pipeline()
            // 1. IdleStateHandler: Timeout/KeepAlive 감지 (Hot Deploy 설정 값 반영)
            /* .addLast(new IdleStateHandler(
                    properties.getReaderIdleTime(),
                    properties.getWriterIdleTime(),
                    properties.getAllIdleTime(),
                    TimeUnit.SECONDS)) */
            
            // 1-2. DelimiterBasedFrameDecoder: 구분자 기반 패킷 분리
            // 예: 최대 길이 8192, 줄바꿈 문자(\n)를 구분자로 사용
            // (주의: maxFrameLength와 delimiter는 실제 프로토콜에 맞게 설정해야 합니다)
            /*
            .addLast(new DelimiterBasedFrameDecoder(
                8192, // Max Frame Length (너무 긴 패킷 방지)
                Delimiters.lineDelimiter() // 구분자 (예: \r\n, \n)
            )) */

            // Raw logging for incoming bytes to help debug encoding/frame issues
            //.addLast(new com.example.tcp.netty.handler.RawLoggingHandler())
   
            // 2. Codec: 문자열 기반 (실무에서는 ByteArrayDecoder 등을 사용)
            //.addLast(new StringDecoder(CP949_CHARSET))
            //.addLast(new StringEncoder(CP949_CHARSET))
            //.addLast(new StringDecoder(CharsetUtil.UTF_8))
            //.addLast(new StringEncoder(CharsetUtil.UTF_8))

            // 뒤이어 처리할 디코더 및 핸들러 추가
            .addLast(testDecoder)
            .addLast(testHandler);
            
            // 3. Business Logic
            //.addLast(new BusinessLogicHandler(sessionManager, properties));
    }
}