package com.example.tcp.controller;

import com.example.tcp.service.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/message")
@RequiredArgsConstructor
public class MessageController {

    private final SessionManager sessionManager;

    // 모든 클라이언트에게 메시지 전송
    @PostMapping("/broadcast")
    public String broadcastMessage(@RequestBody String message) {
        log.info("Request to broadcast message: {}", message);
        sessionManager.broadcast(message);
        return "Broadcast success: " + message;
    }

    // 현재 접속 중인 클라이언트 ID 목록 조회
    @GetMapping("/clients")
    public Set<String> getConnectedClients() {
        return sessionManager.getAllClientIds();
    }

    // 특정 클라이언트에게 메시지 전송
    @PostMapping("/send/{clientId}")
    public String sendMessageToClient(@PathVariable String clientId, @RequestBody String message) {
        log.info("Request to send message to client {}: {}", clientId, message);
        boolean success = sessionManager.sendToClient(clientId, message);
        if (success) {
            return "Message sent to " + clientId;
        } else {
            return "Failed to send message. Client not found or inactive.";
        }
    }
}
