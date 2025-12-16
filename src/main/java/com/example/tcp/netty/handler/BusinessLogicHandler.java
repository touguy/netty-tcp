package com.example.tcp.netty.handler;

import com.example.tcp.config.NettyProperties;
import com.example.tcp.service.SessionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class BusinessLogicHandler extends SimpleChannelInboundHandler<String> {

    private final SessionManager sessionManager;
    private final NettyProperties properties;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 접속 시도 시 SessionManager 정책 검사
        if (!sessionManager.registerSession(ctx.channel())) {
            log.warn("Connection rejected by Session Policy.");
            ctx.writeAndFlush("ERR: Server Busy\n");
            ctx.close();
        } else {
            log.info("Client connected: {}", ctx.channel().remoteAddress());
            ctx.writeAndFlush("WELCOME to TCP Gateway\n");
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
                ctx.writeAndFlush(properties.getKeepAliveMessage());
                
                // 만약 엄격한 Timeout을 원한다면:
                // ctx.close(); 
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        // 실제 데이터 수신 로직 (Async 처리가 필요하면 여기서 CompletableFuture 등 사용)
        log.info("Received message: {}", msg);
        
        if ("PING".equalsIgnoreCase(msg.trim())) {
            // 클라이언트가 보낸 PING에 대한 응답
            ctx.writeAndFlush("PONG\n");
        } else {
            // 일반 메시지 처리 (Echo)
            try {
                // 여기에 비즈니스 로직 추가 (DB 조회, 외부 API 호출 등)
                // 예: String response = businessService.processMessage(msg);
                
                // 간단한 Echo 응답 (한글 포함)
                ctx.writeAndFlush("ECHO: " + msg + "\n");
            } catch (Exception e) {
                log.error("Error processing message", e);
                ctx.writeAndFlush("ERR: Internal Server Error\n");
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Exception caught", cause);
        ctx.close();
    }
}