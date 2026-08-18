package com.zhangdaing.ae2online.web;

import io.javalin.Javalin;
import io.javalin.http.HandlerType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zhangdaing.ae2online.UI.TokenRegistry;

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

                // 配置路由
                // 返回存储物品列表
                config.routes.get("/storage", ctx -> {
                String auth = ctx.header("Authorization");
                String token = auth != null && auth.startsWith("Bearer ") ? auth.substring(7) : null;
                var terminal = TokenRegistry.getTerminal(token);
                if (terminal == null) {
                    ctx.status(401).json(Map.of("error", "登录过期"));
                    return;
                }
                var grid = terminal.getGrid();
                if (grid == null){
                    ctx.json(Map.of("error", "no terminal connected to a network"));
                    return;
                }
                var storage = grid.getStorageService().getCachedInventory();
                var items = storage.keySet().stream().map(
                    key -> {
                        var id = key.getId();
                        return Map.of(
                            "id", key.getDisplayName().getString(),
                            "count", storage.get(key),
                            "icon", "/icon/" + id.getNamespace() + "/" + id.getPath(),
                            "modid", id.getNamespace(),
                            "amountPerUnit", key.getAmountPerUnit(),
                            "rawId", id.toString()
                        );
                    }
                ).toList();
                ctx.json(Map.of("items", items));
                });

                // 返回图标
                config.routes.get("/icon/{modid}/{itemid}", ctx -> {
                    String modid = ctx.pathParam("modid");
                    String itemid = ctx.pathParam("itemid");
                    String iconPath = com.zhangdaing.ae2online.Config.iconPath;
                    java.nio.file.Path icon = java.nio.file.Paths.get("./" + iconPath, modid+ "__" + itemid + ".png");
                    if (java.nio.file.Files.exists(icon)) {
                        ctx.contentType("image/png");
                        ctx.result(java.nio.file.Files.readAllBytes(icon));
                    } else {
                        ctx.status(404);
                    }
                });

                // 登录
                config.routes.post("/login", ctx -> {
                    var body = ctx.bodyAsClass(Map.class);
                    String username = (String) body.get("username");
                    String password = (String) body.get("password");

                    var terminal = TerminalRegistry.findByUsername(username);
                    if (terminal == null || !password.equals(terminal.getPassword())) {
                        ctx.status(401).result("认证失败");
                        return;
                    }
                    String token = TokenRegistry.createToken(terminal);
                    ctx.json(Map.of("token", token));
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
}
