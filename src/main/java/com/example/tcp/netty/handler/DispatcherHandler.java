package com.example.tcp.netty.handler;

import com.example.tcp.service.SessionManager;
import com.example.tcp.proto.PacketProto.Packet;
import com.example.tcp.proto.PacketProto.Packet.PacketType;
import com.example.tcp.proto.WrapperProto.WrapperMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ChannelHandler.Sharable // 이 핸들러는 여러 채널에서 공유 가능해야 함 (빈 주입 시)
@RequiredArgsConstructor
public class DispatcherHandler extends SimpleChannelInboundHandler<WrapperMessage> {

    private final SessionManager sessionManager;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 1. 전체 세션(TCP 연결) 수 제한 검사
        if (!sessionManager.registerSession(ctx.channel())) {
            log.warn("Connection rejected (Server Busy): {}", ctx.channel().remoteAddress());

            // [중요] 에러 패킷 생성
            Packet errorPacket = Packet.newBuilder()
                    .setType(PacketType.ERROR)
                    .setStatusCode(503)
                    .setPayload("Server Busy: Too many connections")
                    .build();

            // [중요] Wrapper로 감싸서 전송 후 연결 종료
            ctx.writeAndFlush(WrapperMessage.newBuilder().setPacket(errorPacket).build());
            ctx.close();
        } else {
            log.info("Client connected: {}", ctx.channel().remoteAddress());
            super.channelActive(ctx);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 연결 끊김 시 세션 관리자에서 제거
        sessionManager.removeSession(ctx.channel());
        log.info("Client disconnected: {}", ctx.channel().remoteAddress());
        super.channelInactive(ctx);
    }    

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            
            // 읽기 유휴 상태 (클라이언트로부터 요청이 오랫동안 없을 때)
            if (event.state() == IdleState.READER_IDLE) {
                log.info("Reader Idle (No Request). Sending PING...");
                
                // 1. PING 패킷 생성
                Packet pingPacket = Packet.newBuilder()
                        .setType(PacketType.PING)
                        .setTimestamp(System.currentTimeMillis())
                        .setPayload("KeepAlive Probe")
                        .build();

                // 2. [중요] WrapperMessage로 감싸기
                WrapperMessage wrapperMsg = WrapperMessage.newBuilder()
                        .setPacket(pingPacket)
                        .build();

                // 3. 전송
                ctx.writeAndFlush(wrapperMsg);
                
                // (선택사항) 만약 PING을 보내는 게 아니라 바로 끊으려면:
                // ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Exception caught", cause);
        ctx.close();
    }    

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WrapperMessage wrapper) {
        // WrapperMessage 내부의 oneof 필드 중 무엇이 설정되었는지 확인
        switch (wrapper.getMsgCase()) {
            case PACKET:
                // Packet 객체만 꺼내서 다음 핸들러(PacketHandler)로 전달
                ctx.fireChannelRead(wrapper.getPacket());
                break;
                
            case AUTH:
                // AuthMessage 객체만 꺼내서 다음 핸들러(AuthHandler)로 전달
                ctx.fireChannelRead(wrapper.getAuth());
                break;
                
            case ORDER:
                // OrderMessage 객체만 꺼내서 다음 핸들러(OrderHandler)로 전달
                ctx.fireChannelRead(wrapper.getOrder());
                break;
                
            case MSG_NOT_SET:
            default:
                log.error("Received empty or unknown wrapper message");
                ctx.close();
                break;
        }
    }
}