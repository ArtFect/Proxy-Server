package ru.fiw.proxyserver;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

public class GuiProxy extends Screen {
    private boolean isSocks4 = false;

    private TextFieldWidget ipPort;
    private TextFieldWidget username;
    private TextFieldWidget password;
    private CheckboxWidget enabledCheck;

    private Screen parentScreen;

    private String msg = "";

    private int[] positionY;
    private int positionX;

    private TestPing testPing = new TestPing();

    public GuiProxy(Screen parentScreen) {
        super(new LiteralText("Proxy"));
        this.parentScreen = parentScreen;
    }

    private boolean checkProxy() {
        if (!isValidIpPort(ipPort.getText())) {
            msg = Formatting.RED + "Invalid IP:PORT";
            this.ipPort.changeFocus(true);
            return false;
        }
        return true;
    }

    private static boolean isValidIpPort(String ipP) {
        return ipP.matches("(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5]):[0-9]+");
    }

    private void centerButtons(int amount, int buttonLength, int gap) {
        positionX = (this.width / 2) - (buttonLength / 2);
        positionY = new int[amount];
        int center = (this.height + amount * gap) / 2;
        int buttonStarts = center - (amount * gap);
        for (int i = 0; i != amount; i++) {
            positionY[i] = buttonStarts + (gap * i);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        super.keyPressed(keyCode, scanCode, modifiers);
        msg = "";
        testPing.state = "";
        return true;
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);

        if (enabledCheck.isChecked() && !isValidIpPort(ipPort.getText())) {
            enabledCheck.onPress();
        }

        drawStringWithShadow(matrixStack, this.textRenderer, "Proxy Type:", this.width / 2 - 149, positionY[1] + 5, 10526880);
        drawCenteredString(matrixStack, this.textRenderer, "Proxy Authentication (optional)", this.width / 2, positionY[3] + 8, Formatting.WHITE.getColorValue());
        drawStringWithShadow(matrixStack, this.textRenderer, "IP:PORT: ", this.width / 2 - 125, positionY[2] + 5, 10526880);

        this.ipPort.render(matrixStack, mouseX, mouseY, partialTicks);
        if (isSocks4) {
            drawStringWithShadow(matrixStack, this.textRenderer, "User ID: ", this.width / 2 - 140, positionY[4] + 5, 10526880);
            this.username.render(matrixStack, mouseX, mouseY, partialTicks);
        } else {
            drawStringWithShadow(matrixStack, this.textRenderer, "Username: ", this.width / 2 - 140, positionY[4] + 5, 10526880);
            drawStringWithShadow(matrixStack, this.textRenderer, "Password: ", this.width / 2 - 140, positionY[5] + 5, 10526880);
            this.username.render(matrixStack, mouseX, mouseY, partialTicks);
            this.password.render(matrixStack, mouseX, mouseY, partialTicks);
        }

        drawCenteredString(matrixStack, this.textRenderer, !msg.isEmpty() ? msg : testPing.state, this.width / 2, positionY[6] + 5, 10526880);

        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public void tick() {
        testPing.pingPendingNetworks();

        this.ipPort.tick();
        this.username.tick();
        this.password.tick();
    }

    @Override
    public void init() {
        MinecraftClient.getInstance().keyboard.setRepeatEvents(true);
        int buttonLength = 160;
        centerButtons(10, buttonLength, 26);

        isSocks4 = ProxyServer.proxy.type == Proxy.ProxyType.SOCKS4;

        ButtonWidget proxyType = new ButtonWidget(positionX, positionY[1], buttonLength, 20, new LiteralText(isSocks4 ? "Socks 4" : "Socks 5"), (button) -> {
            isSocks4 = !isSocks4;
            button.setMessage(new LiteralText(isSocks4 ? "Socks 4" : "Socks 5"));
        });
        this.addButton(proxyType);

        this.ipPort = new TextFieldWidget(this.textRenderer, positionX, positionY[2], buttonLength, 20, new LiteralText(""));
        this.ipPort.setText(ProxyServer.proxy.ipPort);
        this.ipPort.setMaxLength(21);
        this.ipPort.changeFocus(true);
        this.children.add(this.ipPort);

        this.username = new TextFieldWidget(this.textRenderer, positionX, positionY[4], buttonLength, 20, new LiteralText(""));
        this.username.setMaxLength(255);
        this.username.setText(ProxyServer.proxy.username);
        this.children.add(this.username);

        this.password = new TextFieldWidget(this.textRenderer, positionX, positionY[5], buttonLength, 20, new LiteralText(""));
        this.password.setMaxLength(255);
        this.password.setText(ProxyServer.proxy.password);
        this.children.add(this.password);

        int posXButtons = (this.width / 2) - (((buttonLength / 2) * 3) / 2);

        ButtonWidget apply = new ButtonWidget(posXButtons, positionY[8], buttonLength / 2 - 3, 20, new LiteralText("Apply"), (button) -> {
            if (checkProxy()) {
                ProxyServer.proxy = new Proxy(isSocks4, ipPort.getText(), username.getText(), password.getText());
                AccountsProxy.setDefaultProxy(ProxyServer.proxy);
                AccountsProxy.saveProxyAccounts();
                ProxyServer.proxyEnabled = enabledCheck.isChecked();
                MinecraftClient.getInstance().openScreen(new MultiplayerScreen(new TitleScreen()));
            }
        });
        this.addButton(apply);

        ButtonWidget test = new ButtonWidget(posXButtons + buttonLength / 2 + 3, positionY[8], buttonLength / 2 - 3, 20, new LiteralText("Test"), (button) -> {
            if (ipPort.getText().isEmpty() || ipPort.getText().equalsIgnoreCase("none")) {
                msg = Formatting.RED + "Specify proxy to test";
                return;
            }
            if (checkProxy()) {
                testPing = new TestPing();
                testPing.run("mc.hypixel.net", 25565, new Proxy(isSocks4, ipPort.getText(), username.getText(), password.getText()));
            }
        });
        this.addButton(test);

        this.enabledCheck = new CheckboxWidget((this.width / 2) - (15 + textRenderer.getWidth("Proxy Enabled")) / 2, positionY[7], buttonLength, 20, new LiteralText("Proxy Enabled"), ProxyServer.proxyEnabled);
        this.addButton(this.enabledCheck);

        ButtonWidget cancel = new ButtonWidget(posXButtons + (buttonLength / 2 + 3) * 2, positionY[8], buttonLength / 2 - 3, 20, new LiteralText("Cancel"), (button) -> {
            MinecraftClient.getInstance().openScreen(parentScreen);
        });
        this.addButton(cancel);
    }

    @Override
    public void onClose() {
        msg = "";
        MinecraftClient.getInstance().keyboard.setRepeatEvents(false);
    }
}