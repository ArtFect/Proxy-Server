package ru.fiw.proxyserver;

import com.mojang.realmsclient.gui.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraftforge.fml.client.config.GuiCheckBox;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.io.IOException;
import java.net.UnknownHostException;

final class GuiProxy extends GuiScreen {
    private GuiCheckBox socks4;
    private GuiCheckBox socks5;
    private GuiTextField ipPort;

    private GuiTextField username;
    private GuiTextField userID;
    private GuiTextField password;

    private GuiScreen parentScreen;

    private String msg = "";

    private Integer[] positionY;
    private int positionX;

    private Proxy oldProxy;

    private TestPing testPing = new TestPing();

    GuiProxy(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
        this.fontRenderer = Minecraft.getMinecraft().fontRenderer;
    }

    private boolean setProxy() {
        String ipPortS = ipPort.getText();
        if (ipPortS.isEmpty() || ipPortS.equalsIgnoreCase("none") || ipPortS.startsWith("127.0.0.1")) {
            ProxyServer.proxy = new Proxy(socks4.isChecked(),"", 0, userID.getText(), username.getText(), password.getText());
            return true;
        }
        if (!isValidIpPort(ipPortS)) {
            msg = ChatFormatting.RED + "Invalid IP:PORT";
            ipPort.setFocused(true);
            return false;
        }

        String[] spl = ipPortS.split(":");
        ProxyServer.proxy = new Proxy(socks4.isChecked(), spl[0], Integer.parseInt(spl[1]), userID.getText(), username.getText(), password.getText());
        return true;
    }

    private static boolean isValidIpPort(String ipP) {
        return ipP.matches("(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5]):[0-9]+");
    }

    private void centerButtons(int amount, int buttonLength, int gap) {
        positionX = (this.width / 2) - (buttonLength / 2);
        positionY = new Integer[amount];
        int center = (this.height + amount * gap) / 2;
        int buttonStarts = center - (amount * gap);
        for (int i = 0; i != amount; i++) {
            positionY[i] = buttonStarts + (gap * i);
        }
    }

    @Override
    protected void actionPerformed(GuiButton b) throws UnknownHostException {
        switch (b.id) {
            case 1: //Socks4 check
                if (!socks5.isChecked()) {
                    socks4.setIsChecked(true);
                } else {
                    socks4.setIsChecked(true);
                    socks5.setIsChecked(false);
                }
                break;
            case 2: //Socks5 check
                if (!socks4.isChecked()) {
                    socks5.setIsChecked(true);
                } else {
                    socks5.setIsChecked(true);
                    socks4.setIsChecked(false);
                }
                break;
            case 4: //Test
                if (setProxy()) {
                    testPing = new TestPing();
                    testPing.run("mc.hypixel.net", 25565, ProxyServer.proxy.ip);
                }
                break;
            case 6: //Apply
                if (setProxy()) {
                    Config.saveProxy(ProxyServer.proxy);
                    this.mc.displayGuiScreen(new GuiMultiplayer(new GuiMainMenu()));
                }
                break;
            case 7: //Cancel
                ProxyServer.proxy = oldProxy;
                this.mc.displayGuiScreen(parentScreen);
                break;
        }
    }

    @Override
    protected void keyTyped(char c, int k) throws IOException {
        super.keyTyped(c, k);
        this.ipPort.textboxKeyTyped(c, k);
        this.username.textboxKeyTyped(c, k);
        this.password.textboxKeyTyped(c, k);
        msg = "";
        testPing.state = "";
    }

    @Override
    protected void mouseClicked(int x, int y, int b) throws IOException {
        super.mouseClicked(x, y, b);
        this.ipPort.mouseClicked(x, y, b);
        this.username.mouseClicked(x, y, b);
        this.password.mouseClicked(x, y, b);
    }

    @Override
    public void drawScreen(int p_73863_1_, int p_73863_2_, float p_73863_3_) {
        this.drawDefaultBackground();

        this.drawString(this.fontRenderer, "Proxy Type:", this.width / 2 - 149, positionY[1] + 5, 10526880);
        this.drawString(this.fontRenderer, "IP:PORT", this.width / 2 - 125, positionY[2] + 5, 10526880);
        this.drawCenteredString(this.fontRenderer, "Authentication (optional)", this.width / 2, positionY[3] + 8, Color.WHITE.getRGB());

        this.ipPort.drawTextBox();
        if (socks5.isChecked()) {
            this.drawString(this.fontRenderer, "Username: ", this.width / 2 - 140, positionY[4] + 5, 10526880);
            this.drawString(this.fontRenderer, "Password: ", this.width / 2 - 140, positionY[5] + 5, 10526880);
            this.username.drawTextBox();
            this.password.drawTextBox();
        } else {
            this.drawString(this.fontRenderer, "User ID: ", this.width / 2 - 140, positionY[4] + 5, 10526880);
            this.userID.drawTextBox();
        }


        this.drawCenteredString(this.fontRenderer, !msg.isEmpty() ? msg : testPing.state, this.width / 2, positionY[6] + 5, 10526880);
        super.drawScreen(p_73863_1_, p_73863_2_, p_73863_3_);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        this.ipPort.drawTextBox();
        this.username.drawTextBox();
        if (socks5.isChecked()) {
            this.password.drawTextBox();
        }

        testPing.pingPendingNetworks();
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        int buttonLength = 160;
        centerButtons(10, buttonLength, 26);
        this.socks4 = new GuiCheckBox(1, positionX + 10, positionY[1] + 5, "Socks4", ProxyServer.proxy.type == ProxyServer.ProxyType.SOCKS4);
        this.buttonList.add(this.socks4);

        this.socks5 = new GuiCheckBox(2, positionX + 100, positionY[1] + 5, "Socks5", ProxyServer.proxy.type == ProxyServer.ProxyType.SOCKS5);
        this.buttonList.add(this.socks5);

        this.ipPort = new GuiTextField(3, this.fontRenderer, positionX, positionY[2], buttonLength, 20);
        this.ipPort.setText(ProxyServer.proxy.enabled ? ProxyServer.proxy.ip + ":" + ProxyServer.proxy.port : "");
        this.ipPort.setMaxStringLength(21);
        this.ipPort.setFocused(true);

        this.username = new GuiTextField(4, this.fontRenderer, positionX, positionY[4], buttonLength, 20);
        this.username.setMaxStringLength(255);
        this.username.setText(ProxyServer.proxy.username);

        this.userID = new GuiTextField(8, this.fontRenderer, positionX, positionY[4], buttonLength, 20);
        this.userID.setMaxStringLength(255);
        this.userID.setText(ProxyServer.proxy.userID);

        this.password = new GuiTextField(5, this.fontRenderer, positionX, positionY[5], buttonLength, 20);
        this.password.setMaxStringLength(255);
        this.password.setText(ProxyServer.proxy.password);

        GuiButton apply = new GuiButton(6, positionX, positionY[7], buttonLength / 2 - 3, 20, "Apply");
        this.buttonList.add(apply);

        GuiButton test = new GuiButton(4, positionX + buttonLength / 2 + 3, positionY[7], buttonLength / 2 - 3, 20, "Test");
        this.buttonList.add(test);

        GuiButton cancel = new GuiButton(7, positionX, positionY[8], buttonLength, 20, "Cancel");
        this.buttonList.add(cancel);

        oldProxy = ProxyServer.proxy;
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        msg = "";
        Keyboard.enableRepeatEvents(false);
    }
}