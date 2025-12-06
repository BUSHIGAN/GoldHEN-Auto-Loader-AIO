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
    public int top = 40;

    private Image buttonX;
    private Image buttonO;
    private Image background;
	
    private String[] menuItems = {
        "Start LAPSE (9.00 > 12.02)",
        "Start POOPS (9.00 > 12.52)"
    };

    private int selected = 0;
    private int countdown = 10;
    private String firmware = "";

    public Screen(ArrayList messages)
    {
        this.messages = messages;
        font = new Font(null, Font.PLAIN, 32);

        Toolkit tk = Toolkit.getDefaultToolkit();
        buttonX    = tk.getImage("/disc/BDMV/AUXDATA/X.png");
        buttonO    = tk.getImage("/disc/BDMV/AUXDATA/O.png");
        background = tk.getImage("/disc/BDMV/AUXDATA/background.png");

        MediaTracker mt = new MediaTracker(this);
        if (buttonX != null)    mt.addImage(buttonX, 0);
        if (buttonO != null)    mt.addImage(buttonO, 1);
        if (background != null) mt.addImage(background, 2);
        try {
            mt.waitForAll();
        } catch (InterruptedException e) {
            
        }
    }

    public void setFirmware(String fw) { firmware = fw; }

    public void moveSelection(int dir) {
        selected += dir;
        if (selected < 0) selected = menuItems.length - 1;
        if (selected >= menuItems.length) selected = 0;
        repaint();
    }

    public int getSelected() { return selected; }

    public void setCountdown(int c) {
        countdown = c;
        repaint();
    }

    public void paint(Graphics g)
    {
        
        if (background != null)
            g.drawImage(background, 0, 0, getWidth(), getHeight(), this);
        else {
            g.setColor(new Color(26, 26, 26));
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        
        g.setFont(new Font(null, Font.PLAIN, 48));
        String title = "GoldHEN AUTO-LOADER | LAPSE + POOPS (A.I.O) v1.3";
        int tw = g.getFontMetrics().stringWidth(title);

        
        g.setColor(Color.BLACK);
        g.drawString(title, (getWidth() - tw) / 2 + 3, 63);

        
        g.setColor(Color.WHITE);
        g.drawString(title, (getWidth() - tw) / 2, 60);

        
        int menuWidth  = 500;
        int menuX      = 40;
        int menuY      = 110;
        int menuHeight = getHeight() - 200;

        g.setColor(new Color(0,0,0,180));
        g.fillRoundRect(menuX, menuY, menuWidth, menuHeight, 20,20);

        g.setColor(new Color(90,90,90));
        g.drawRoundRect(menuX, menuY, menuWidth, menuHeight, 20,20);

        
        g.setFont(new Font(null, Font.PLAIN, 32));
        g.setColor(Color.ORANGE);
        g.drawString("Auto-loader : " + countdown + "s", menuX + 20, menuY + 40);

        
        g.setFont(new Font(null, Font.PLAIN, 32));
        int startY = menuY + 110;

        for (int i = 0; i < menuItems.length; i++) {
            int y = startY + i * 55;

            if (i == selected) {
                g.setColor(new Color(255,190,60,180));
                g.fillRoundRect(menuX + 10, y - 30, menuWidth - 20, 42, 12, 12);
                g.setColor(Color.BLACK);
            } else {
                g.setColor(Color.WHITE);
            }

            g.drawString(menuItems[i], menuX + 30, y);
        }

        
        int iconY = menuY + menuHeight - 150;

        g.setFont(new Font(null, Font.PLAIN, 28));
        g.setColor(new Color(220,220,220));

        
        g.drawString("PRESS", menuX + 30, iconY);

        if (buttonX != null)
            g.drawImage(buttonX, menuX + 135, iconY - 32, 36, 36, this);

        g.drawString("TO SELECT MENU", menuX + 185, iconY);

        
        g.drawString("PRESS", menuX + 30, iconY + 55);

        if (buttonO != null)
            g.drawImage(buttonO, menuX + 135, (iconY + 55) - 32, 36, 36, this);

        g.drawString("TO QUIT", menuX + 185, iconY + 55);

        
        int logX = menuX + menuWidth + 40;
        int logY = 110;
        int logW = getWidth() - logX - 40;
        int logH = getHeight() - 200;

        g.setColor(new Color(0,0,0,180));
        g.fillRoundRect(logX, logY, logW, logH, 20,20);

        g.setColor(new Color(90,90,90));
        g.drawRoundRect(logX, logY, logW, logH, 20,20);

        
        g.setFont(font);
        g.setColor(Color.WHITE);

        int lineY = logY + 50;
        for(int i = 0; i < messages.size(); i++) {
            g.drawString((String)messages.get(i), logX + 20, lineY + i * 36);
        }

        
        g.setFont(new Font(null, Font.PLAIN, 28));
        g.setColor(Color.WHITE);
        if (firmware != null)
            g.drawString("FW: " + firmware, logX + logW - 150, logY + logH - 25);
    }
}
