package com.example.tcp.service;

import com.example.tcp.config.NettyProperties;
import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionManager {

    private final NettyProperties properties;

    // 모든 연결된 채널을 관리하는 그룹 (Netty 제공)
    private final ChannelGroup allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    // 채널 ID와 접속 시간을 매핑하여 "오래된 세션" 찾기용
    private final Map<ChannelId, Long> channelTimes = new ConcurrentHashMap<>();

    // String ID로 채널을 조회하기 위한 맵
    private final Map<String, Channel> sessionMap = new ConcurrentHashMap<>();

    /**
     * 세션 등록 시도 (Over-session 정책 적용)
     * 
     * @return true: 접속 허용, false: 접속 거부
     */
    public boolean registerSession(Channel channel) {
        int currentCount = allChannels.size();
        int max = properties.getMaxSessions();

        if (currentCount >= max) {
            log.warn("Max sessions reached ({}/{}). Policy: {}", currentCount, max,
                    properties.isRejectNewIfFull() ? "REJECT_NEW" : "KICK_OLD");

            if (properties.isRejectNewIfFull()) {
                // 정책: 신규 접속 거부
                return false;
            } else {
                // 정책: 가장 오래된 세션 강제 종료 (Kick Old)
                kickOldestSession();
            }
        }

        allChannels.add(channel);
        channelTimes.put(channel.id(), System.currentTimeMillis());
        sessionMap.put(channel.id().asLongText(), channel);
        log.info("New Session Registered. Total: {}", allChannels.size());
        return true;
    }

    public void removeSession(Channel channel) {
        allChannels.remove(channel);
        channelTimes.remove(channel.id());
        sessionMap.remove(channel.id().asLongText());
        log.info("Session Removed. Total: {}", allChannels.size());
    }

    private void kickOldestSession() {
        ChannelId oldestId = null;
        long minTime = Long.MAX_VALUE;

        for (Map.Entry<ChannelId, Long> entry : channelTimes.entrySet()) {
            if (entry.getValue() < minTime) {
                minTime = entry.getValue();
                oldestId = entry.getKey();
            }
        }

        if (oldestId != null) {
            Channel oldestChannel = allChannels.find(oldestId);
            if (oldestChannel != null) {
                log.info("Kicking oldest session: {}", oldestChannel.remoteAddress());
                oldestChannel.close(); // 강제 종료
                // removeSession은 channelInactive에서 호출됨
            }
        }
    }

    /**
     * 모든 접속자에게 메시지 전송
     * 
     * @param message 전송할 메시지
     */
    public void broadcast(String message) {
        log.info("Broadcasting message to {} clients: {}", allChannels.size(), message);
        allChannels.writeAndFlush(message + "\n");
    }

    /**
     * 특정 클라이언트에게 메시지 전송
     * 
     * @param clientId 대상 클라이언트 ID (ChannelId.asLongText())
     * @param message  전송할 메시지
     * @return 전송 성공 여부
     */
    public boolean sendToClient(String clientId, String message) {
        Channel channel = sessionMap.get(clientId);
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(message + "\n");
            log.info("Sent message to client {}: {}", clientId, message);
            return true;
        }
        log.warn("Client not found or inactive: {}", clientId);
        return false;
    }

    /**
     * 현재 접속 중인 모든 클라이언트 ID 반환
     */
    public java.util.Set<String> getAllClientIds() {
        return sessionMap.keySet();
    }

    public int getCurrentSessionCount() {
        return allChannels.size();
    }
}