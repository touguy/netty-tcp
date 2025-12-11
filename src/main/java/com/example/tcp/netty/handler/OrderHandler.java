package com.example.tcp.netty.handler;

import com.example.tcp.proto.OrderProto.OrderMessage;
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
public class OrderHandler extends SimpleChannelInboundHandler<OrderMessage> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, OrderMessage msg) {
        log.info("[ORDER] ID: {}", msg.getOrderId());
        
        // 1. PING 패킷 생성
        Packet response = Packet.newBuilder()
                .setType(PacketType.DATA)
                .setPayload("Order Accepted")
                .build();

        // 2. [중요] WrapperMessage로 감싸기
        WrapperMessage wrapperMsg = WrapperMessage.newBuilder()
                .setPacket(response)
                .build();

        // 3. 전송
        ctx.writeAndFlush(wrapperMsg);        
    }

}