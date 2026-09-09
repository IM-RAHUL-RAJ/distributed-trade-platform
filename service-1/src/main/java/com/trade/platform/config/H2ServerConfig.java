package com.trade.platform.config;

import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Embedded H2 (file at /data/trade_platform) runs inside Service 1 and exposes a
 * TCP endpoint so Service 2 can reach the same database from its own JVM.
 * Activate with H2_TCP_ENABLED=true (used by docker-compose).
 */
@Configuration
public class H2ServerConfig {

    private static final Logger log = LoggerFactory.getLogger(H2ServerConfig.class);

    @Bean
    public ApplicationRunner h2TcpServer(
            @Value("${app.h2.tcp.enabled:false}") boolean enabled,
            @Value("${app.h2.tcp.port:9095}") int port) {
        return args -> {
            if (!enabled) {
                log.info("H2 TCP server disabled (app.h2.tcp.enabled=false)");
                return;
            }
            try (Connection c = DriverManager.getConnection(
                    "jdbc:h2:file:/data/trade_platform;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
                    "sa", "")) {
                // touch the DB so the file exists before the TCP server binds
            }
            Server server = Server.createTcpServer(
                    "-tcp", "-tcpPort", String.valueOf(port),
                    "-tcpAllowOthers", "-baseDir", "/data");
            server.start();
            log.info("H2 TCP server listening on port {} (share /data/trade_platform)", port);
        };
    }
}