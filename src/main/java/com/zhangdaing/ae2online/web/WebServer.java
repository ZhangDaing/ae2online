package com.zhangdaing.ae2online.web;

import io.javalin.Javalin;
import io.javalin.http.HandlerType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class WebServer {
    private static final Logger LOGGER = LoggerFactory.getLogger("AE2Online Web");

    private static Javalin app;

    // 启动Web服务器
    public static void startsever(){
        // 如果正在运行
        if (app != null) {
            return;
        }


        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
        // 切换为当前类的类加载器
            Thread.currentThread().setContextClassLoader(WebServer.class.getClassLoader());

        
            // 创建Javalin实例
            app = Javalin.create(config -> {
                // 配置跨域请求
                config.routes.before(ctx -> {
                String origin = ctx.header("Origin");
                // 非游览器请求，通过
                if (origin == null){
                    return;
                }
                // 只允许本地请求
                if (origin.startsWith("http://localhost") || origin.startsWith("http://127.0.0.1")){
                    ctx.header("Access-Control-Allow-Origin", origin);
                    ctx.header("Access-Control-Allow-Credentials", "true");
                    //
                    if (ctx.method() == HandlerType.OPTIONS) {
                        ctx.header("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                        ctx.header("Access-Control-Allow-Headers", "Content-Type");
                        ctx.status(204);
                        return;
                    }
                return;
                }
                // 非本地请求，拒绝
                ctx.status(403).result("Forbidden");
            });
                //
                config.routes.get("/storage", ctx -> {
                var grid = findGrid();
                if (grid == null){
                    ctx.json(Map.of("error", "no terminal connected to a network"));
                    return;
                }
                var storage = grid.getStorageService().getCachedInventory();
                var items = storage.keySet().stream().map(
                    key -> Map.of(
                        "id", key.getDisplayName().getString(),
                        "count", storage.get(key)))
                    .toList();
                ctx.json(Map.of("items", items));
            });
            LOGGER.info("Web server started on port 11595");
            }).start(11595); // 启动服务器，监听11595端口
        }
        finally {
            // 恢复原来的类加载器
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    // 停止Web服务器
    public static void stop() {
        if (app != null) {
            app.stop();
            app = null;
            LOGGER.info("Web server stopped");
        }
    }


    private static appeng.api.networking.IGrid findGrid() {
        var terminal = TerminalRegistry.getAllTerminals().values().stream()
        .filter(t -> t.isOnline() && t.getGrid() != null)
        .findFirst()
        .orElse(null);
        return terminal != null ? terminal.getGrid() : null;
    }
}
