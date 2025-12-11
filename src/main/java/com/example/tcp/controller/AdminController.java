package com.example.tcp.controller;

import com.example.tcp.config.NettyProperties;
import com.example.tcp.service.SessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final NettyProperties properties;
    private final SessionManager sessionManager;

    // 현재 설정 조회
    @GetMapping("/config")
    public NettyProperties getConfig() {
        return properties;
    }

    // 설정 변경 (Hot Deploy) - Max Session, Timeout 등
    @PostMapping("/config")
    public String updateConfig(@RequestBody Map<String, Object> updates) {
        if (updates.containsKey("maxSessions")) {
            properties.setMaxSessions((Integer) updates.get("maxSessions"));
        }
        if (updates.containsKey("rejectNewIfFull")) {
            properties.setRejectNewIfFull((Boolean) updates.get("rejectNewIfFull"));
        }
        if (updates.containsKey("readerIdleTime")) {
            properties.setReaderIdleTime((Integer) updates.get("readerIdleTime"));
        }
        return "Configuration updated. Some settings apply to NEW connections only.";
    }

    // 현재 세션 현황 조회
    @GetMapping("/sessions")
    public String getSessions() {
        return "Current Sessions: " + sessionManager.getCurrentSessionCount();
    }
}