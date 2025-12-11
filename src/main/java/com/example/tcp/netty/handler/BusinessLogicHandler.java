package com.example.tcp.netty.handler;

import com.example.tcp.config.NettyProperties;
import com.example.tcp.service.SessionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.example.tcp.proto.PacketProto.Packet;
import com.example.tcp.proto.PacketProto.Packet.PacketType;

@Slf4j
@RequiredArgsConstructor
public class BusinessLogicHandler extends SimpleChannelInboundHandler<Packet> {

    private final SessionManager sessionManager;
    private final NettyProperties properties;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 접속 시도 시 SessionManager 정책 검사
        if (!sessionManager.registerSession(ctx.channel())) {
            log.warn("Connection rejected: {}", ctx.channel().remoteAddress());
            ctx.writeAndFlush(Packet.newBuilder()
                    .setType(PacketType.ERROR)
                    .setStatusCode(503)
                    .setPayload("Server Busy")
                    .build());
            ctx.close();

        } else {
            log.info("Client connected: {}", ctx.channel().remoteAddress());
            //ctx.writeAndFlush("WELCOME to TCP Gateway\n");
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 연결 끊김 처리
        sessionManager.removeSession(ctx.channel());
        log.info("Client disconnected: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // IdleStateEvent (Timeout / Polling) 처리
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                // 일정 시간 읽기 데이터가 없으면 -> Polling(KeepAlive) 확인 또는 종료
                log.info("Reader Idle (No Request). Sending KeepAlive Probe...");
                // 여기서는 PING을 보내거나, 바로 끊을 수도 있음. 설정에 따라 다름.
                ctx.writeAndFlush(Packet.newBuilder()
                        .setType(PacketType.PING)
                        .setTimestamp(System.currentTimeMillis())
                        .build());                
                // 만약 엄격한 Timeout을 원한다면:
                //ctx.close(); 
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet msg) throws Exception {
        // 실제 데이터 수신 로직 (Async 처리가 필요하면 여기서 CompletableFuture 등 사용)
        switch (msg.getType()) {
            case PING:
                log.debug("Received PING");
                Packet pongPacket = Packet.newBuilder()
                        .setType(PacketType.PONG)
                        .setTimestamp(System.currentTimeMillis())
                        .build();                
                ctx.writeAndFlush(pongPacket);
                break;
            case DATA:
                log.info("Data received: {}", msg.getPayload());
                // Echo Logic
                Packet echoPacket = Packet.newBuilder()
                        .setType(PacketType.DATA)
                        .setPayload("ECHO: " + msg.getPayload())
                        .build();
                ctx.writeAndFlush(echoPacket);
                break;
            default:
                log.debug("Ignored message type: {}", msg.getType());
                break;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Exception caught", cause);
        ctx.close();
    }
}