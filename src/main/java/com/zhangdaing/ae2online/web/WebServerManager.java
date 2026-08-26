package com.zhangdaing.ae2online.web;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class WebServerManager {
	private static final Logger LOGGER = LoggerFactory.getLogger("WebServerManager");
    private static final ConcurrentHashMap<Integer, WebServer> servers = new ConcurrentHashMap<>();

    public static synchronized void startServer(int port) {
        if (port <= 0 || port > 65535) return;
        WebServer existing = servers.get(port);
        if (existing != null && existing.isRunning()) {
            return;
        }
        if (existing != null) {
            servers.remove(port);
        }
        try {
            WebServer server = new WebServer(port);
            server.start();
            servers.put(port, server);
            LOGGER.info("WebServer started on port {}", port);
        } catch (IOException e) {
            LOGGER.error("Failed to start WebServer on port {}", port, e);
        }
    }

    public static void stopServer(int port) {
        WebServer server = servers.remove(port);
        if (server != null) {
            server.stop();
            LOGGER.info("WebServer stopped on port {}", port);
        }
    }

    public static void stopAll() {
        for (int port : servers.keySet()) {
            stopServer(port);
        }
    }

    public static boolean isRunning(int port) {
        WebServer server = servers.get(port);
        return server != null && server.isRunning();
    }
}
