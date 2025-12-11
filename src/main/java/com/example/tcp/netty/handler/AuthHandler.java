package com.example.tcp.netty.handler;

import com.example.tcp.proto.AuthProto.AuthMessage;
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
public class AuthHandler extends SimpleChannelInboundHandler<AuthMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AuthMessage msg) {
        log.info("[AUTH] User: {}", msg.getUserId());
        
        // 1. PING 패킷 생성
        Packet response = Packet.newBuilder()
                .setType(PacketType.DATA)
                .setPayload("Welcome " + msg.getUserId())
                .build();

        // 2. [중요] WrapperMessage로 감싸기
        WrapperMessage wrapperMsg = WrapperMessage.newBuilder()
                .setPacket(response)
                .build();

        // 3. 전송
        ctx.writeAndFlush(wrapperMsg);        
    }        

}