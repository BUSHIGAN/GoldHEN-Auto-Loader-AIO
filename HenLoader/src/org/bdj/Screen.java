package org.bdj;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.util.ArrayList;

public class Screen extends Container
{
    private static final long serialVersionUID = 4761178503523947426L;

    private ArrayList messages;
    private Font font;

    private Image buttonX;
    private Image buttonO;
    private Image buttonS;
    private Image background;

    private boolean autoLoadEnabled = false;

    private String[] menuItems = {
        "Start LAPSE (9.00 > 12.02)",
        "Start POOPS (9.00 > 12.52)",
        "Copy GoldHEN v2.4b18.7 to PS4",
        "Start Kernel Dumper",
        "Disable Updates",
        "Homebrew / Payload Launcher",
        "USB Payload Browser",
        "Auto-Loader ON/OFF"
    };

    private int selected = 0;
    private int countdown = 1;
    private String firmware = "";

    public Screen(ArrayList messages)
    {
        this.messages = messages;
        this.font = new Font(null, Font.PLAIN, 32);

        Toolkit tk = Toolkit.getDefaultToolkit();

        buttonX = tk.getImage("/disc/BDMV/AUXDATA/X.png");
        buttonO = tk.getImage("/disc/BDMV/AUXDATA/O.png");
        buttonS = tk.getImage("/disc/BDMV/AUXDATA/S.png");
        background = tk.getImage("/disc/BDMV/AUXDATA/background.png");

        MediaTracker mt = new MediaTracker(this);
        mt.addImage(buttonX, 0);
        mt.addImage(buttonO, 1);
        mt.addImage(buttonS, 2);
        mt.addImage(background, 3);

        try { mt.waitForAll(); } catch (InterruptedException e) {}
    }

    public void setFirmware(String fw) { firmware = fw; }

    public void setAutoLoadEnabled(boolean enabled) {
        autoLoadEnabled = enabled;
        repaint();
    }

    public int getSelected() { return selected; }

    public void moveSelection(int dir) {
        selected += dir;
        if (selected < 0) selected = menuItems.length - 1;
        if (selected >= menuItems.length) selected = 0;
        repaint();
    }

    public void setCountdown(int c) {
        countdown = c;
        repaint();
    }

    public void paint(Graphics g)
    {
        if (background != null)
            g.drawImage(background, 0, 0, getWidth(), getHeight(), this);
        else {
            g.setColor(Color.black);
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        String title = "GoldHEN AUTO-LOADER | LAPSE + POOPS (A.I.O) [Deluxe 1.5]";
        g.setFont(new Font(null, Font.BOLD, 48));

        int tw = g.getFontMetrics().stringWidth(title);
        g.setColor(Color.BLACK);
        g.drawString(title, (getWidth() - tw) / 2 + 3, 63);
        g.setColor(Color.WHITE);
        g.drawString(title, (getWidth() - tw) / 2, 60);

        int menuX = 40;
        int menuY = 110;
        int menuWidth = 500;
        int menuHeight = getHeight() - 200;

        g.setColor(new Color(0,0,0,180));
        g.fillRoundRect(menuX, menuY, menuWidth, menuHeight, 20,20);
        g.setColor(new Color(120,120,120));
        g.drawRoundRect(menuX, menuY, menuWidth, menuHeight, 20,20);

        g.setFont(new Font(null, Font.PLAIN, 32));
        g.setColor(Color.ORANGE);
        g.drawString("Auto-Loader : " + (autoLoadEnabled ? "ON" : "OFF") +
                     " (" + countdown + "s)", menuX + 20, menuY + 40);

        int startY = menuY + 110;
        int i = 0;

        while (i < menuItems.length) {
            int y = startY + i * 55;

            if (i == selected) {
                g.setColor(new Color(255,200,60,180));
                g.fillRoundRect(menuX + 10, y - 30, menuWidth - 20, 42, 15, 15);
                g.setColor(Color.BLACK);
            } else {
                g.setColor(Color.WHITE);
            }

            g.drawString(menuItems[i], menuX + 30, y);
            i++;
        }

        int iconY = menuY + menuHeight - 150;

        g.setColor(Color.LIGHT_GRAY);
        g.setFont(new Font(null, Font.PLAIN, 28));

        g.drawString("PRESS", menuX + 30, iconY);
        g.drawImage(buttonX, menuX + 135, iconY - 32, 36, 36, this);
        g.drawString("TO SELECT", menuX + 185, iconY);

        g.drawString("PRESS", menuX + 30, iconY + 55);
        g.drawImage(buttonO, menuX + 135, iconY + 23, 36, 36, this);
        g.drawString("TO EXIT", menuX + 185, iconY + 55);

        g.drawString("PRESS", menuX + 30, iconY + 110);
        g.drawImage(buttonS, menuX + 135, iconY + 78, 36, 36, this);
        g.drawString("TO CLEAR LOGS", menuX + 185, iconY + 110);

        int logX = menuX + menuWidth + 40;
        int logY = 110;
        int logW = getWidth() - logX - 40;
        int logH = getHeight() - 200;

        g.setColor(new Color(0,0,0,180));
        g.fillRoundRect(logX, logY, logW, logH, 20,20);
        g.setColor(new Color(120,120,120));
        g.drawRoundRect(logX, logY, logW, logH, 20,20);

        g.setFont(font);
        g.setColor(Color.WHITE);

        int lineY = logY + 50;
        int idx = 0;

        while (idx < messages.size()) {
            g.drawString((String) messages.get(idx), logX + 20, lineY + idx * 36);
            idx++;
        }

        g.setFont(new Font(null, Font.PLAIN, 28));
        g.drawString("FW: " + firmware, logX + logW - 150, logY + logH - 25);
    }
}
