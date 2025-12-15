package com.example.tcp.netty;

import com.example.tcp.config.NettyProperties;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class NettyTcpServer {

    private final NettyProperties properties;
    private final NettyChannelInitializer channelInitializer;
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture serverChannelFuture;

    @PostConstruct
    public void start() {
        new Thread(() -> {
            // Boss 쓰레드: 연결 수락 담당 (1개면 충분)
            // Worker 쓰레드: 데이터 I/O 담당 (기본값: CPU 코어 * 2)
            bossGroup = new NioEventLoopGroup(properties.getBossThread());
            workerGroup = new NioEventLoopGroup(properties.getWorkerThread());

            try {
                ServerBootstrap b = new ServerBootstrap();
                b.group(bossGroup, workerGroup)
                 .channel(NioServerSocketChannel.class)
                 .childHandler(channelInitializer)  // Keep-alive 유지
                 .option(ChannelOption.SO_BACKLOG, 128)  // 대기열 크기
                 .childOption(ChannelOption.SO_KEEPALIVE, true);  // Keep-alive 유지

                int port = properties.getPort();
                serverChannelFuture = b.bind(port).sync();
                log.info("=== Netty TCP Server Started on port: {} ===", port);
                
                serverChannelFuture.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Netty Server interrupted", e);
            } finally {
                stop();
            }
        }).start();
    }

    @PreDestroy
    public void stop() {
        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
        log.info("Netty TCP Server Stopped.");
    }
}