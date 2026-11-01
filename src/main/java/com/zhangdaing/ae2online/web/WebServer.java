package com.zhangdaing.ae2online.web;

import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingSimulationRequester;
import appeng.api.networking.security.IActionHost;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.me.helpers.MachineSource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;

import com.zhangdaing.ae2online.UI.TokenRegistry;
import com.zhangdaing.ae2online.blockentity.OnlineTerminalBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebServer {
    private static final Logger LOGGER = LoggerFactory.getLogger("AE2Online Web");
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();
    private static final Pattern ICON_RE = Pattern.compile("^/icon/(.+)$");

    private final HttpServer server;
    private final int port;
    private final boolean https;
    private volatile boolean running = false;

    public WebServer(int port) throws IOException {
        this.port = port;
        // 监听地址来自配置：默认 0.0.0.0（局域网可访问），改 127.0.0.1 则仅本机
        String bind = com.zhangdaing.ae2online.Config.bindAddress;
        if (bind == null || bind.isBlank()) {
            LOGGER.warn("Config.bindAddress 未加载（为 null/空），回退到 127.0.0.1。");
            bind = "127.0.0.1";
        }
        InetSocketAddress addr = new InetSocketAddress(bind.trim(), port);

        // HTTPS 模式：Godot Web 版需要安全上下文才能启动
        boolean wantHttps = com.zhangdaing.ae2online.Config.enableHttps;
        HttpsServer httpsServer = null;
        if (wantHttps) {
            httpsServer = tryCreateHttps(addr);
        }
        this.https = httpsServer != null;

        if (httpsServer != null) {
            this.server = httpsServer;
            LOGGER.info("WebServer 已启动（HTTPS）: https://{}:{}", bind.trim(), port);
        } else {
            if (wantHttps) {
                LOGGER.warn("HTTPS 启动失败，已回退 HTTP。浏览器中的 Godot Web 版将无法运行（安全上下文限制）。");
            }
            this.server = HttpServer.create(addr, 0);
            LOGGER.info("WebServer 已启动（HTTP）: http://{}:{}", bind.trim(), port);
        }
        server.createContext("/", (exchange) -> handle(exchange));
        server.setExecutor(Executors.newFixedThreadPool(8));
    }

    /** 尝试创建 HTTPS 服务器；证书不可用时返回 null 由调用方回退 HTTP */
    private static HttpsServer tryCreateHttps(InetSocketAddress addr) {
        try {
            java.nio.file.Path configDir = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get();
            java.security.KeyStore ks = SelfSignedCert.ensure(configDir);
            if (ks == null) {
                LOGGER.error("无法准备证书，HTTPS 不可用（详见上方日志）");
                return null;
            }
            String pass = new String(SelfSignedCert.STORE_PASS);
            javax.net.ssl.KeyManagerFactory kmf =
                    javax.net.ssl.KeyManagerFactory.getInstance(javax.net.ssl.KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, pass.toCharArray());
            javax.net.ssl.SSLContext ctx = javax.net.ssl.SSLContext.getInstance("TLS");
            ctx.init(kmf.getKeyManagers(), null, null);

            HttpsServer s = HttpsServer.create(addr, 0);
            s.setHttpsConfigurator(new com.sun.net.httpserver.HttpsConfigurator(ctx));
            return s;
        } catch (Throwable t) {
            LOGGER.error("创建 HTTPS 服务器失败", t);
            return null;
        }
    }

    public void start() {
        server.start();
        running = true;
    }

    public void stop() {
        try {
            server.stop(0);
        } catch (Exception e) {
            LOGGER.debug("停止 WebServer 异常（可忽略）", e);
        }
        running = false;
    }

    public int getPort() { return port; }
    public boolean isRunning() { return running; }
    public boolean isHttps() { return https; }

    private void handle(HttpExchange ex) throws IOException {
        try {
            String origin = ex.getRequestHeaders().getFirst("Origin");
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
                cors(ex, origin);
                ex.sendResponseHeaders(204, -1);
                return;
            }
            if (origin != null && !originAllowed(origin)) {
                sendText(ex, 403, "Forbidden");
                return;
            }
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();
            if ("GET".equals(method) && "/storage".equals(path)) storage(ex);
            else if ("GET".equals(method) && "/craftable".equals(path)) craftable(ex);
            else if ("GET".equals(method) && "/cpu".equals(path)) cpu(ex);
            else if ("POST".equals(method) && "/login".equals(path)) login(ex);
            else if ("POST".equals(method) && "/cpu/cancel".equals(path)) cpuCancel(ex);
            else if ("POST".equals(method) && "/craft/plan".equals(path)) craftPlan(ex);
            else if ("POST".equals(method) && "/craft".equals(path)) craft(ex);
            else {
                Matcher m = ICON_RE.matcher(path);
                if ("GET".equals(method) && m.matches()) icon(ex, m.group(1));
                else if ("GET".equals(method)) staticResource(ex, path);
                else sendText(ex, 404, "Not Found");
            }
        } catch (Exception e) {
            LOGGER.error("Error handling {} {}", ex.getRequestMethod(), ex.getRequestURI(), e);
        } finally {
            ex.close();
        }
    }

    // ---------- 工具 ----------
    /**
     * 读取请求体里的 count 字段。
     * 缺失或非数字时返回 -1，让调用方按“参数错误”返回 400，而不是抛 NPE 掉进通用异常处理。
     */
    private long readCount(Map<String, Object> b) {
        Object v = b.get("count");
        return v instanceof Number n ? n.longValue() : -1L;
    }

    private String token(HttpExchange ex) {
        String a = ex.getRequestHeaders().getFirst("Authorization");
        return a != null && a.startsWith("Bearer ") ? a.substring(7) : null;
    }

    /**
     * 判断浏览器来源是否允许。
     *
     * 规则：
     *   1. 同源请求无条件放行 —— 内嵌 Web UI 就是从本服务加载的页面，
     *      它发的请求 Origin 必然是 https://<本机地址>:<端口>，必须允许，
     *      否则 Web 版无法登录/取数据（此前只匹配 http:// 导致 HTTPS 下 403）。
     *   2. localhost / 127.0.0.1 的 http 与 https 均放行。
     *   3. Config.allowedOrigins 里的条目作为额外白名单。
     *
     * Godot 桌面/移动客户端不发 Origin 头，走 origin == null 的分支，不受影响。
     */
    private boolean originAllowed(String origin) {
        if (origin == null || origin.isBlank()) return false;

        // 同源：Origin 的主机端口与本服务监听的地址一致
        if (isSameOrigin(origin)) return true;

        // 本机（http / https 都认）
        String lower = origin.toLowerCase();
        if (lower.startsWith("http://localhost") || lower.startsWith("https://localhost")
                || lower.startsWith("http://127.0.0.1") || lower.startsWith("https://127.0.0.1")) {
            return true;
        }

        var extra = com.zhangdaing.ae2online.Config.allowedOrigins;
        if (extra == null) return false;
        for (String allowed : extra) {
            if (allowed != null && !allowed.isBlank() && origin.equalsIgnoreCase(allowed.trim())) return true;
        }
        return false;
    }

    /** 判断 Origin 是否指向本服务的端口（同源） */
    private boolean isSameOrigin(String origin) {
        try {
            java.net.URI uri = new java.net.URI(origin);
            int p = uri.getPort();
            if (p == -1) {
                p = "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
            }
            // 端口一致即认为是访问本服务的页面
            return p == this.port;
        } catch (Exception e) {
            return false;
        }
    }

    private void cors(HttpExchange ex, String origin) {
        if (origin != null && originAllowed(origin)) {
            ex.getResponseHeaders().set("Access-Control-Allow-Origin", origin);
            ex.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");
            ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        }
    }

    private void sendJson(HttpExchange ex, int status, Object obj) throws IOException {
        byte[] body = GSON.toJson(obj).getBytes(StandardCharsets.UTF_8);
        cors(ex, ex.getRequestHeaders().getFirst("Origin"));
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, body.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(body); }
    }

    private void sendText(HttpExchange ex, int status, String text) throws IOException {
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        cors(ex, ex.getRequestHeaders().getFirst("Origin"));
        ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        ex.sendResponseHeaders(status, body.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(body); }
    }

    private Map<String, Object> body(HttpExchange ex) throws IOException {
        String raw = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        if (raw.isBlank()) return new HashMap<>();
        return GSON.fromJson(raw, MAP_TYPE);
    }

    // ---------- 接口 ----------
    private void storage(HttpExchange ex) throws IOException {
        var t = TokenRegistry.getTerminal(token(ex));
        if (t == null) { sendJson(ex, 401, Map.of("error", "登录过期")); return; }
        var grid = t.getGrid();
        if (grid == null) { sendJson(ex, 200, Map.of("error", "no terminal connected to a network")); return; }
        var inventory = grid.getStorageService().getCachedInventory();
        // 必须直接迭代 KeyCounter，而不是 keySet()。
        //
        // AE2 的终端按 KeyCounter 的迭代顺序把条目下发给客户端
        // （MEStorageMenu -> MEInventoryUpdatePacket.addChanges(availableStacks, ...)），
        // 而数量排序没有次级比较器，数量相同时的顺序完全取决于这个下发顺序。
        // keySet() 返回的是 HashSet（无序、按哈希），顺序与 AE2 不一致，
        // 会导致「数量排序时并列项顺序与原版 AE2 不同」。
        var items = new java.util.ArrayList<Map<String, Object>>(inventory.size());
        for (var entry : inventory) {
            var key = entry.getKey();
            var id = key.getId();
            items.add(Map.of(
                "id", key.getDisplayName().getString(),
                "count", entry.getLongValue(),
                "icon", "/icon/" + urlEncode(iconFilename(key)),
                "modid", id.getNamespace(),
                "amountPerUnit", key.getAmountPerUnit(),
                // uid 含 NBT，用于区分同物品不同变体（如不同伪装板）；rawId 仅 item id，供合成接口使用
                "uid", uidOf(key),
                "rawId", id.toString()));
        }
        sendJson(ex, 200, Map.of("items", items));
    }

    /**
     * 图标文件名中的非法字符替换，规则与 icon-exporter 模组的 ImageExportUtil.escapeKey 保持一致：
     *   ":" → "__"， "\"" → "'"， "/" → "___"
     * 例如伪装板图标：ae2__facade__{item__'minecraft__oak_log'}.png
     */
    private static String escapeKey(String s) {
        return s.replace(":", "__").replace("\"", "'").replace("/", "___");
    }

    /**
     * 生成 icon-exporter 导出的图标文件名。
     * 无 NBT：modid__itemid.png；有 NBT：modid__itemid__{nbt}.png
     * 之前只用 modid__itemid.png，导致伪装板等带 NBT 物品命中不到图标（显示空白）。
     */
    private static String iconFilename(appeng.api.stacks.AEKey key) {
        var id = key.getId();
        StringBuilder sb = new StringBuilder(64);
        sb.append(escapeKey(id.toString()));
        if (key instanceof appeng.api.stacks.AEItemKey itemKey && itemKey.getTag() != null) {
            sb.append("__").append(escapeKey(itemKey.getTag().toString()));
        }
        sb.append(".png");
        return sb.toString();
    }

    /**
     * 物品唯一标识（含 NBT），前端用它做卡片 key。
     * 只用 item id 会让多个伪装板合并成一个条目。
     */
    private static String uidOf(appeng.api.stacks.AEKey key) {
        return key.toTagGeneric().toString();
    }

    /** URL 路径段编码（空格等字符需编码为百分号形式）。 */
    private static String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
        } catch (Exception e) {
            return s;
        }
    }

    private void icon(HttpExchange ex, String filename) throws IOException {
        // 防路径穿越：拒绝目录分隔符与 ..
        if (filename.isEmpty() || filename.length() > 255
                || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            sendText(ex, 400, "Bad Request");
            return;
        }
        Path icon = Paths.get("./" + com.zhangdaing.ae2online.Config.iconPath, filename);
        if (!Files.exists(icon)) {
            // 回退：去掉 NBT 后缀（__{...}）再找基础图标。
            // icon-exporter 只导出特定 NBT 状态的图标（如满电 20000 的便携元件），
            // 实际物品的 NBT 状态往往不同（部分电量、内存卡存了数据），此时回退到无 NBT 版本；
            // 伪装板等 NBT 精确命中的仍走上面的精确匹配，不受影响。
            String plain = filename.replaceFirst("__\\{.*\\}\\.png$", ".png");
            if (!plain.equals(filename)) {
                Path fallback = Paths.get("./" + com.zhangdaing.ae2online.Config.iconPath, plain);
                if (Files.exists(fallback)) icon = fallback;
            }
        }
        if (Files.exists(icon)) {
            byte[] data = Files.readAllBytes(icon);
            // Web 版（跑在浏览器里）通过 HTTPRequest 取图标属于跨源请求，
            // 必须带 CORS 头，否则浏览器会丢弃响应导致图标全部空白。
            cors(ex, ex.getRequestHeaders().getFirst("Origin"));
            ex.getResponseHeaders().set("Content-Type", "image/png");
            // 图标内容不变，允许浏览器缓存，减少重复请求
            ex.getResponseHeaders().set("Cache-Control", "public, max-age=86400");
            ex.sendResponseHeaders(200, data.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(data); }
        } else {
            cors(ex, ex.getRequestHeaders().getFirst("Origin"));
            sendText(ex, 404, "Not Found");
        }
    }

    /** jar 内 Web UI 资源的根路径（Web 版 jar 才有，标准版为 null） */
    private static final String WEB_RESOURCE_ROOT = "/assets/ae2online/web/";

    /**
     * 提供内嵌 Web UI 静态资源。
     *
     * 同一份代码同时支持两种 jar：
     *   - Web 版：jar 内含 assets/ae2online/web/*，访问 / 返回 index.html
     *   - 标准版：资源不存在，返回一段说明文字，提示使用桌面/移动客户端
     */
    private void staticResource(HttpExchange ex, String path) throws IOException {
        // 根路径映射到 index.html
        String rel = "/".equals(path) ? "index.html" : path.substring(1);

        // 防路径穿越
        if (rel.isEmpty() || rel.contains("..") || rel.contains("\\")) {
            sendText(ex, 400, "Bad Request");
            return;
        }

        byte[] data = readWebResource(rel);
        if (data == null) {
            // 标准版 jar：没有内嵌 Web UI
            if ("index.html".equals(rel)) {
                sendHtml(ex, 200, noWebUiPage());
            } else {
                sendText(ex, 404, "Not Found");
            }
            return;
        }

        // 内嵌 Web UI 一般是同源加载，但显式带上 CORS 更稳妥
        // （例如把页面挂到别的源、或用 file:// 打开时也能工作）
        cors(ex, ex.getRequestHeaders().getFirst("Origin"));
        ex.getResponseHeaders().set("Content-Type", mimeType(rel));
        // wasm / pck 体积较大，允许浏览器缓存
        if (rel.endsWith(".wasm") || rel.endsWith(".pck")) {
            ex.getResponseHeaders().set("Cache-Control", "public, max-age=3600");
        }
        ex.sendResponseHeaders(200, data.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(data); }
    }

    /** 从 jar 内读取 Web UI 资源；不存在返回 null（说明是标准版 jar） */
    private static byte[] readWebResource(String relPath) {
        try (var in = WebServer.class.getResourceAsStream(WEB_RESOURCE_ROOT + relPath)) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException e) {
            LOGGER.warn("读取内嵌 Web 资源失败: {}", relPath, e);
            return null;
        }
    }

    private static String mimeType(String name) {
        String n = name.toLowerCase();
        if (n.endsWith(".html")) return "text/html; charset=utf-8";
        if (n.endsWith(".js") || n.endsWith(".mjs")) return "text/javascript; charset=utf-8";
        if (n.endsWith(".wasm")) return "application/wasm";
        if (n.endsWith(".pck")) return "application/octet-stream";
        if (n.endsWith(".css")) return "text/css; charset=utf-8";
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".svg")) return "image/svg+xml";
        if (n.endsWith(".json")) return "application/json; charset=utf-8";
        if (n.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }

    private void sendHtml(HttpExchange ex, int status, String html) throws IOException {
        byte[] body = html.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        ex.sendResponseHeaders(status, body.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(body); }
    }

    /** 标准版 jar 访问根路径时显示的说明页 */
    private static String noWebUiPage() {
        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>AE2 Online</title>
                  <style>
                    body { font-family: system-ui, -apple-system, "Segoe UI", sans-serif;
                           background: #1e1e1e; color: #ddd; margin: 0;
                           display: flex; align-items: center; justify-content: center;
                           min-height: 100vh; }
                    .card { max-width: 520px; padding: 32px; line-height: 1.7; }
                    h1 { color: #4ec9b0; font-size: 22px; margin: 0 0 16px; }
                    code { background: #2d2d2d; padding: 2px 6px; border-radius: 3px; color: #ce9178; }
                    .hint { color: #888; font-size: 14px; margin-top: 20px; }
                  </style>
                </head>
                <body>
                  <div class="card">
                    <h1>AE2 Online — API 服务运行中</h1>
                    <p>当前安装的是<strong>标准版</strong>模组，未内置 Web 界面。</p>
                    <p>如需在浏览器中直接使用，请改用 <code>ae2online-*-web.jar</code>。</p>
                    <p>也可以使用桌面客户端或 Android 客户端连接：</p>
                    <p><code>http://本机IP:端口</code></p>
                    <div class="hint">API 接口仍可正常使用（/login、/storage、/cpu 等）。</div>
                  </div>
                </body>
                </html>
                """;
    }

    private void login(HttpExchange ex) throws IOException {
        Map<String, Object> b = body(ex);
        String u = (String) b.get("username");
        String p = (String) b.get("password");
        OnlineTerminalBlockEntity matched = null;
        for (var entry : TerminalRegistry.getAllTerminals().entrySet()) {
            OnlineTerminalBlockEntity t = entry.getValue();
            if (u != null && u.equals(t.getUsername()) && p != null && p.equals(t.getPassword())) {
                matched = t;
                break;
            }
        }
        if (matched == null) {
            // 区分「凭据不对」与「终端根本没接入网络」：后者时注册表为空，
            // 只回「认证失败」会让用户误以为账号密码写错了
            if (TerminalRegistry.getAllTerminals().isEmpty()) {
                sendJson(ex, 503, Map.of("error",
                        "没有在线的终端：请确认线上终端方块已接入 ME 网络并有可用频道"));
                return;
            }
            sendText(ex, 401, "认证失败");
            return;
        }
        // 双重保险：注册表里的终端理应在线，若此刻已掉线则明确拒绝
        if (!matched.isOnline()) {
            sendJson(ex, 503, Map.of("error", "终端已离线：请检查 ME 网络供电与频道"));
            return;
        }
        sendJson(ex, 200, Map.of("token", TokenRegistry.createToken(matched)));
    }

    private void craftable(HttpExchange ex) throws IOException {
        var t = TokenRegistry.getTerminal(token(ex));
        if (t == null) { sendJson(ex, 401, Map.of("error", "登录过期")); return; }
        var grid = t.getGrid();
        if (grid == null) { sendJson(ex, 200, Map.of("error", "no terminal connected to a network")); return; }
        var items = grid.getCraftingService().getCraftables(AEItemKey.filter()).stream().map(key -> {
            var id = key.getId();
            return Map.of("id", key.getDisplayName().getString(), "rawId", id.toString(),
                "modid", id.getNamespace(), "icon", "/icon/" + urlEncode(iconFilename(key)));
        }).toList();
        sendJson(ex, 200, Map.of("items", items));
    }

    private void cpu(HttpExchange ex) throws IOException {
        var t = TokenRegistry.getTerminal(token(ex));
        if (t == null) { sendJson(ex, 401, Map.of("error", "登录过期")); return; }
        var grid = t.getGrid();
        if (grid == null) { sendJson(ex, 200, Map.of("error", "no terminal connected to a network")); return; }
        AtomicInteger idx = new AtomicInteger();
        var cpus = grid.getCraftingService().getCpus().stream().map(cpu -> {
            Map<String, Object> m = new HashMap<>();
            m.put("index", idx.getAndIncrement());
            m.put("busy", cpu.isBusy());
            m.put("storage", cpu.getAvailableStorage());
            m.put("coProcessors", cpu.getCoProcessors());
            m.put("name", cpu.getName() == null ? null : cpu.getName().getString());
            if (cpu.getJobStatus() != null) {
                var st = cpu.getJobStatus();
                Map<String, Object> job = new HashMap<>();
                job.put("what", st.crafting().what().getDisplayName().getString());
                job.put("amount", st.crafting().amount());
                job.put("progress", st.progress() / (double) st.totalItems() * st.crafting().amount());
                job.put("elapsedTimeNanos", st.elapsedTimeNanos());
                m.put("job", job);
            } else m.put("job", null);
            return m;
        }).toList();
        sendJson(ex, 200, Map.of("cpus", cpus));
    }

    private void cpuCancel(HttpExchange ex) throws IOException {
        var t = TokenRegistry.getTerminal(token(ex));
        if (t == null) { sendJson(ex, 401, Map.of("error", "登录过期")); return; }
        var grid = t.getGrid();
        if (grid == null) { sendJson(ex, 200, Map.of("error", "no terminal connected to a network")); return; }
        Object idx = body(ex).get("index");
        if (!(idx instanceof Number n)) { sendJson(ex, 400, Map.of("error", "参数错误")); return; }
        var cpus = List.copyOf(grid.getCraftingService().getCpus());
        int i = n.intValue();
        if (i < 0 || i >= cpus.size()) { sendJson(ex, 400, Map.of("error", "CPU索引无效")); return; }
        var cpu = cpus.get(i);
        if (cpu.isBusy()) { cpu.cancelJob(); sendJson(ex, 200, Map.of("success", true)); }
        else sendJson(ex, 200, Map.of("success", false, "error", "CPU当前无任务"));
    }

    private void craftPlan(HttpExchange ex) throws IOException {
        var t = TokenRegistry.getTerminal(token(ex));
        if (t == null) { sendJson(ex, 401, Map.of("error", "登录过期")); return; }
        var grid = t.getGrid();
        if (grid == null) { sendJson(ex, 200, Map.of("error", "no terminal connected to a network")); return; }
        Map<String, Object> b = body(ex);
        String rawId = (String) b.get("rawId");
        long amount = readCount(b);
        if (rawId == null || amount <= 0) { sendJson(ex, 400, Map.of("error", "参数错误")); return; }
        var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(rawId));
        if (item == null) { sendJson(ex, 400, Map.of("error", "无效的物品ID")); return; }
        ICraftingPlan plan;
        try {
            plan = plan(grid, AEItemKey.of(item), amount);
        } catch (InterruptedException e) { sendJson(ex, 499, Map.of("error", "计算已被取消")); return; }
        catch (Exception e) { sendJson(ex, 500, Map.of("error", "合成计算异常: " + e.getMessage())); return; }
        var out = plan.finalOutput().what();
        Map<String, Object> resp = new HashMap<>();
        resp.put("id", out.getDisplayName().getString());
        resp.put("count", plan.finalOutput().amount());
        resp.put("bytes", plan.bytes());
        resp.put("simulation", plan.simulation());
        resp.put("used", list(plan.usedItems()));
        resp.put("emitted", list(plan.emittedItems()));
        resp.put("missing", list(plan.missingItems()));
        sendJson(ex, 200, resp);
    }

    private void craft(HttpExchange ex) throws IOException {
        var t = TokenRegistry.getTerminal(token(ex));
        if (t == null) { sendJson(ex, 401, Map.of("error", "登录过期")); return; }
        var grid = t.getGrid();
        if (grid == null) { sendJson(ex, 200, Map.of("error", "no terminal connected to a network")); return; }
        Map<String, Object> b = body(ex);
        String rawId = (String) b.get("rawId");
        long amount = readCount(b);
        if (rawId == null || amount <= 0) { sendJson(ex, 400, Map.of("error", "参数错误")); return; }
        var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(rawId));
        if (item == null) { sendJson(ex, 400, Map.of("error", "无效的物品ID")); return; }
        ICraftingPlan plan;
        try {
            plan = plan(grid, AEItemKey.of(item), amount);
        } catch (InterruptedException e) { sendJson(ex, 499, Map.of("error", "计算已被取消")); return; }
        catch (Exception e) { sendJson(ex, 500, Map.of("error", "合成计算异常: " + e.getMessage())); return; }
        ICraftingCPU target = null;
        if (b.get("cpu") instanceof Number n) {
            int i = n.intValue();
            var cpus = List.copyOf(grid.getCraftingService().getCpus());
            if (i >= 0 && i < cpus.size()) target = cpus.get(i);
        }
        var result = grid.getCraftingService().submitJob(plan, null, target, target == null, source(grid));
        if (result.successful()) sendJson(ex, 200, Map.of("success", true));
        else sendJson(ex, 400, Map.of("error", String.valueOf(result.errorCode())));
    }

    // ★ 改动：plan、source、list 从 static 改为非静态
    private ICraftingPlan plan(appeng.api.networking.IGrid grid, AEItemKey what, long amount) throws Exception {
        MachineSource src = source(grid);
        ICraftingSimulationRequester req = () -> src;
        return grid.getCraftingService().beginCraftingCalculation(grid.getPivot().getLevel(), req, what, amount,
                CalculationStrategy.REPORT_MISSING_ITEMS).get();
    }

    private MachineSource source(appeng.api.networking.IGrid grid) {
        return new MachineSource(new IActionHost() {
            @Override public IGridNode getActionableNode() { return grid.getPivot(); }
        });
    }

    private List<Map<String, Object>> list(KeyCounter kc) {
        List<Map<String, Object>> l = new ArrayList<>();
        for (var e : kc) {
            var id = e.getKey().getId();
            Map<String, Object> m = new HashMap<>();
            m.put("id", e.getKey().getDisplayName().getString());
            m.put("count", e.getLongValue());
            m.put("rawId", id.toString());
            m.put("icon", "/icon/" + urlEncode(iconFilename(e.getKey())));
            l.add(m);
        }
        return l;
    }
}