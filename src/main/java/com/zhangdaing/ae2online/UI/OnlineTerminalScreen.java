package com.zhangdaing.ae2online.UI;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;

public class OnlineTerminalScreen extends Screen {
    private EditBox usernameBox, passwordBox, urlBox;
    private BlockPos pos;
    private String username;
    private String password;
    private String url;

    public OnlineTerminalScreen(BlockPos pos, String username, String password, String url) {
        super(Component.literal("Terminal Config"));
        this.pos = pos;
        this.username = username;
        this.password = password;
        this.url = url;
    }

    @Override
    protected void init() {
        int w = 200, h = 90;
        int cx = (width - w) / 2, cy = (height - h) / 2;

        String defaultUrl = "http://127.0.0.1:8080";
        String defaultUsername = "admin";

        urlBox = new EditBox(font, cx + 30, cy + 15, w - 20, 20, Component.literal("URL"));
        urlBox.setValue(url != null ? url : defaultUrl);
        usernameBox = new EditBox(font, cx + 30, cy + 40, w - 20, 20, Component.literal("Username"));
        usernameBox.setValue(username != null ? username : defaultUsername);
        passwordBox = new EditBox(font, cx + 30, cy + 65, w - 20, 20, Component.literal("Password"));
        passwordBox.setValue(password);

        addWidget(urlBox); addWidget(usernameBox); addWidget(passwordBox);

        // 保存按钮
        addRenderableWidget(Button.builder(Component.literal("保存"), b -> save())
            .bounds(cx + w - 60, cy + 90, 60, 20).build());
    }

    private void save() {
        // 发送网络包到服务端
        Networking.CHANNEL.sendToServer(
            new UpdateTerminalPacket(pos, usernameBox.getValue(), passwordBox.getValue(), urlBox.getValue()));
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
        g.drawString(font, "URL:", cx, cy + 50, 0xCCCCCC);
        g.drawString(font, "用户名:", cx, cy + 75, 0xCCCCCC);
        g.drawString(font, "密码:", cx, cy + 100, 0xCCCCCC);

        super.render(g, mx, my, partial);
        urlBox.render(g, mx, my, partial);
        usernameBox.render(g, mx, my, partial);
        passwordBox.render(g, mx, my, partial);
    }
}
