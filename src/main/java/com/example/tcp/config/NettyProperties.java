package com.example.tcp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "netty.server")
public class NettyProperties {
    private int port = 8888;
    
    // 세션 관리 설정
    private int maxSessions = 5;          // 최대 접속 허용 수
    private boolean rejectNewIfFull = true; // true: 신규거부, false: 기존해제(Kick Old)
    
    // Timeout 설정 (초 단위)
    private int readerIdleTime = 30;      // 읽기 유휴 시간 (KeepAlive 체크용)
    private int writerIdleTime = 0;
    private int allIdleTime = 0;
    
    // KeepAlive Polling 메시지
    private String keepAliveMessage = "PING\n";
}