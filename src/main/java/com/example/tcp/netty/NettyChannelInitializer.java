package com.example.tcp.netty;

import com.example.tcp.config.NettyProperties;
import com.example.tcp.netty.handler.BusinessLogicHandler;
import com.example.tcp.proto.PacketProto;
import com.example.tcp.service.SessionManager;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;
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
        ChannelPipeline p = ch.pipeline();

        // 1. IdleStateHandler: Timeout/KeepAlive 감지 (Hot Deploy 설정 값 반영)
        p.addLast(new IdleStateHandler(
                properties.getReaderIdleTime(),
                properties.getWriterIdleTime(),
                properties.getAllIdleTime(),
                TimeUnit.SECONDS));
        
        // 2. Protobuf Frame Decoder (길이 정보 처리)
        // 들어오는 패킷의 길이(Varint32)를 읽어서 잘라줍니다.
        // [Inbound] 수신된 바이트의 앞부분(길이 정보)을 읽어 메시지를 자름 (Varint32 방식)
        p.addLast(new ProtobufVarint32FrameDecoder());

        // 3. Protobuf Decoder (바이트 -> 객체 변환)
        // 디코딩할 기본 인스턴스를 지정해야 함
        // [Inbound] 잘린 바이트를 실제 Java 객체(PacketProto.Packet)로 변환
        // (주의: defaultInstance를 넣어줘야 어떤 클래스로 변환할지 Netty가 알 수 있음)
        p.addLast(new ProtobufDecoder(PacketProto.Packet.getDefaultInstance()));

        // 4. Protobuf Length Prepender (나가는 데이터 길이 붙이기)
        // [Outbound] 송신할 때 데이터 앞에 길이 정보를 붙임
        p.addLast(new ProtobufVarint32LengthFieldPrepender());

        // 5. Protobuf Encoder (객체 -> 바이트 변환)
        // [Outbound] Java 객체를 Protobuf 바이트로 직렬화
        p.addLast(new ProtobufEncoder());

        // 6. 비즈니스 로직
        // BusinessLogicHandler는 상태(SessionManager)를 가지지만, 
        // SessionManager 자체가 Thread-safe하다면 매번 new 할 필요 없이 @Sharable을 고려할 수 있음.
        // 여기서는 안전하게 매 채널마다 생성합니다.
        p.addLast(new BusinessLogicHandler(sessionManager, properties));        
    }
}