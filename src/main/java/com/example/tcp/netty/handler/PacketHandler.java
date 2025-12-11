package com.example.tcp.netty.handler;

import com.example.tcp.proto.PacketProto.Packet;
import com.example.tcp.proto.PacketProto.Packet.PacketType;
import com.example.tcp.proto.WrapperProto.WrapperMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ChannelHandler.Sharable
public class PacketHandler extends SimpleChannelInboundHandler<Packet> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet msg) throws Exception {
        // 실제 데이터 수신 로직 (Async 처리가 필요하면 여기서 CompletableFuture 등 사용)
        switch (msg.getType()) {
            case PING:
                log.debug("Received PING");
                // 1. PING 패킷 생성
                Packet pongPacket = Packet.newBuilder()
                        .setType(PacketType.PONG)
                        .setTimestamp(System.currentTimeMillis())
                        .build();                
                ctx.writeAndFlush(WrapperMessage.newBuilder().setPacket(pongPacket).build());
                break;
            case DATA:
                log.info("Data received: {}", msg.getPayload());
                // Echo Logic
                Packet echoPacket = Packet.newBuilder()
                        .setType(PacketType.DATA)
                        .setPayload("ECHO: " + msg.getPayload())
                        .setTimestamp(System.currentTimeMillis())
                        .build();
                ctx.writeAndFlush(WrapperMessage.newBuilder().setPacket(echoPacket).build());
                break;
            default:
                log.debug("Ignored message type: {}", msg.getType());
                break;
        }
    }

}