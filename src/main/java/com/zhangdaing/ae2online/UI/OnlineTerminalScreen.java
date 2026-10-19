package com.zhangdaing.ae2online.UI;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;

public class OnlineTerminalScreen extends Screen {
    private EditBox usernameBox, passwordBox, portBox;
    private BlockPos pos;
    private String username;
    private String password;
    /** 终端保存的端口（旧存档里可能是完整 URL，展示时只取端口数字） */
    private String port;

    public OnlineTerminalScreen(BlockPos pos, String username, String password, String portOrUrl) {
        super(Component.literal("Terminal Config"));
        this.pos = pos;
        this.username = username;
        this.password = password;
        this.port = extractPortText(portOrUrl);
    }

    /**
     * 把存档里的值转成「端口数字」用于显示。
     * 兼容旧存档的完整 URL 格式（http://127.0.0.1:8080 → 8080）。
     */
    private static String extractPortText(String value) {
        if (value == null || value.isBlank()) return "";
        String v = value.trim();
        // 纯数字直接返回
        if (v.matches("\\d{1,5}")) return v;
        // 完整 URL：取端口部分
        try {
            java.net.URI uri = new java.net.URI(v);
            int p = uri.getPort();
            if (p > 0) return String.valueOf(p);
            String scheme = uri.getScheme();
            return "https".equalsIgnoreCase(scheme) ? "443" : "80";
        } catch (Exception e) {
            return "";
        }
    }

    /** 校验失败时的提示文本，为空表示无错误 */
    private String errorText = "";

    @Override
    protected void init() {
        int w = 200, h = 90;
        int cx = (width - w) / 2, cy = (height - h) / 2;

        String defaultPort = "8080";

        // 注意：方块实体里未配置时字段是空字符串 "" 而非 null，
        // 因此必须用 isBlank() 判断，否则默认值永远不会显示。
        // 只有端口给默认值；用户名与密码留空，由使用者自行填写。
        // 端口输入框：只接受数字，保存前会校验范围
        portBox = new EditBox(font, cx + 30, cy + 15, w - 20, 20, Component.literal("Port"));
        portBox.setMaxLength(5);
        portBox.setFilter(s -> s.isEmpty() || s.matches("\\d{0,5}"));
        portBox.setValue(isBlank(port) ? defaultPort : port);
        usernameBox = new EditBox(font, cx + 30, cy + 40, w - 20, 20, Component.literal("Username"));
        usernameBox.setValue(isBlank(username) ? "" : username);
        passwordBox = new EditBox(font, cx + 30, cy + 65, w - 20, 20, Component.literal("Password"));
        passwordBox.setValue(isBlank(password) ? "" : password);

        addWidget(portBox); addWidget(usernameBox); addWidget(passwordBox);

        // 保存按钮
        addRenderableWidget(Button.builder(Component.literal("保存"), b -> save())
            .bounds(cx + w - 60, cy + 90, 60, 20).build());

        // 导出图标按钮（客户端功能；本界面本就是客户端 Screen，直接调用即可）
        addRenderableWidget(Button.builder(Component.literal("导出图标"), b -> exportIcons())
            .bounds(cx + w - 140, cy + 90, 76, 20).build());
    }

    /** 触发图标导出，然后关闭本界面让导出界面接管 */
    private void exportIcons() {
        Boolean started = net.minecraftforge.fml.DistExecutor.unsafeCallWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> com.zhangdaing.ae2online.command.ClientExportBridge.startExport(64, ""));
        if (!Boolean.TRUE.equals(started)) {
            errorText = "导出未能启动（可能已有任务在运行）";
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** 校验端口：必须是 1-65535 的整数 */
    private static String validatePort(String text) {
        if (isBlank(text)) return "请填写端口号";
        String t = text.trim();
        if (!t.matches("\\d{1,5}")) return "端口必须是数字";
        int port = Integer.parseInt(t);
        if (port < 1 || port > 65535) return "端口范围 1-65535";
        return null;
    }

    private void save() {
        String portText = portBox.getValue().trim();
        String err = validatePort(portText);
        if (err != null) {
            // 校验失败：不关闭界面，提示用户改正
            errorText = err;
            return;
        }
        errorText = "";
        // 发送网络包到服务端（字段仍叫 url，保持存档兼容；此处只传端口数字）
        Networking.CHANNEL.sendToServer(
            new UpdateTerminalPacket(pos, usernameBox.getValue(), passwordBox.getValue(), portText));
        onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        int w = 220, h = 150;
        int cx = (width - w) / 2, cy = (height - h) / 2;

        g.fill(cx - 2, cy - 2, cx + w + 2, cy + h + 2, 0x000000);   // 青绿边框
        g.fill(cx, cy, cx + w, cy + h, 0x595959);                    // 深色背景

        g.drawString(font, Component.literal("终端配置"), cx + 8, cy + 10, 0x00FFCC);

        // 标签（浅灰）
        g.drawString(font, "端口:", cx, cy + 50, 0xCCCCCC);
        g.drawString(font, "用户名:", cx, cy + 75, 0xCCCCCC);
        g.drawString(font, "密码:", cx, cy + 100, 0xCCCCCC);

        // 校验错误提示（红色）
        if (!errorText.isEmpty()) {
            g.drawString(font, errorText, cx + 8, cy + h - 12, 0xFF5555);
        }

        super.render(g, mx, my, partial);
        portBox.render(g, mx, my, partial);
        usernameBox.render(g, mx, my, partial);
        passwordBox.render(g, mx, my, partial);
    }
}
