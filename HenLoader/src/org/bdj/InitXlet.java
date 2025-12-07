package org.bdj;

import java.io.*;
import java.util.*;
import javax.tv.xlet.*;
import java.awt.BorderLayout;

import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;

import org.dvb.event.*;
import org.bluray.ui.event.HRcEvent;

import org.bdj.sandbox.DisableSecurityManagerAction;
import org.bdj.external.*;

public class InitXlet implements Xlet, UserEventListener
{
    public static final int BUTTON_X = HRcEvent.VK_ENTER;
    public static final int BUTTON_O = HRcEvent.VK_COLORED_KEY_1;
    public static final int BUTTON_U = HRcEvent.VK_UP;
    public static final int BUTTON_D = HRcEvent.VK_DOWN;
    public static final int BUTTON_S = HRcEvent.VK_COLORED_KEY_2;

    public static InitXlet instance;

    public static class EventQueue {
        private LinkedList list = new LinkedList();
        private int count = 0;

        public synchronized void put(Object o) {
            list.addLast(o);
            count++;
        }

        public synchronized Object get() {
            if (count == 0) return null;
            Object o = list.removeFirst();
            count--;
            return o;
        }
    }

    private EventQueue eq;
    private HScene scene;
    private Screen gui;
    private XletContext context;

    public static final ArrayList messages = new ArrayList();
    private static PrintStream console;

    private Timer autoTimer;
    private int countdown = 1;

    private boolean fwSupportsLapse = false;
    private boolean fwSupportsPoops = false;

    private boolean autoLoadEnabled = false;
    private int autoMode = -1;

    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        try { DisableSecurityManagerAction.execute(); } catch(Exception e){}

        InitXlet.instance = this;
        this.context = ctx;

        eq = new EventQueue();
        scene = HSceneFactory.getInstance().getDefaultHScene();

        try {
            gui = new Screen(messages);
            gui.setSize(1920, 1080);
            scene.add(gui, BorderLayout.CENTER);

            autoLoadEnabled = AutoLoadConfig.loadAutoLoadSetting();
            gui.setAutoLoadEnabled(autoLoadEnabled);

            UserEventRepository repo = new UserEventRepository("events");
            repo.addKey(BUTTON_X);
            repo.addKey(BUTTON_O);
            repo.addKey(BUTTON_U);
            repo.addKey(BUTTON_D);
            repo.addKey(BUTTON_S);
            EventManager.getInstance().addUserEventListener(this, repo);

            final XletContext finalCtx = ctx;

            new Thread(new Runnable() {
                public void run() {
                    try {
                        scene.repaint();
                        console = new PrintStream(new MessagesOutputStream(messages, scene));

                        console.println("");
                        console.println("- GoldHEN 2.4b18.7 by SiSTR0");
                        console.println("- LAPSE by Gezine");
                        console.println("- POOPS by TheFlow0");
                        console.println("- BD-JB SDK by John Tornblom");
                        console.println("- Console Wrapper by Sleirsgoevy");
                        console.println("- Deluxe AIO Menu by Bushigan");
                        console.println("");

                        System.gc();

                        Kernel.initializeKernelOffsets();
                        String fw = Helper.getCurrentFirmwareVersion();
                        gui.setFirmware(fw);
                        console.println("Firmware: " + fw);

                        if (!KernelOffset.hasPS4Offsets()) {
                            console.println("Unsupported firmware");
                            return;
                        }

                        try {
                            float f = Float.parseFloat(fw);
                            if (f >= 9.00f && f <= 12.02f) fwSupportsLapse = true;
                            if (f >= 9.00f && f <= 12.52f) fwSupportsPoops = true;
                        } catch(Exception e){}

                        if (fwSupportsLapse) {
                            autoMode = 0;
                            while (gui.getSelected() != 0) gui.moveSelection(-1);
                            console.println("AUTO-SELECT: LAPSE");
                        } else if (fwSupportsPoops) {
                            autoMode = 1;
                            while (gui.getSelected() != 1) gui.moveSelection(1);
                            console.println("AUTO-SELECT: POOPS");
                        } else {
                            autoMode = -1;
                            console.println("No exploit supported");
                        }

                        console.println("Auto-Loader: " + (autoLoadEnabled ? "ENABLED" : "DISABLED"));

                        if (autoLoadEnabled && autoMode != -1)
                            startAutoCountdown();

                        while (true) {

                            int code = pollInput();

                            if (code == BUTTON_U) {
                                gui.moveSelection(-1);
                                resetAutoCountdown();
                            }
                            else if (code == BUTTON_D) {
                                gui.moveSelection(1);
                                resetAutoCountdown();
                            }
                            else if (code == BUTTON_X) {

                                stopAutoCountdown();
                                int sel = gui.getSelected();

                                if (sel == 0) runSelection(0);
                                else if (sel == 1) runSelection(1);
                                else if (sel == 2) FileHelper.copyPayload(console);
                                else if (sel == 3) TestBinRunner.runTestBin(console);
                                else if (sel == 4) TestBinRunner.runDisableUpdates(console);
                                else if (sel == 5) HomebrewLauncher.openMenu(console);
                                else if (sel == 6) UsbPayloadLauncher.openUSB(console);
                                else if (sel == 7) toggleAutoLoader();

                                scene.repaint();
                            }
                            else if (code == BUTTON_S) {
                                synchronized(messages) {
                                    messages.clear();
                                }
                                console.println("[LOG CLEARED]");
                                scene.repaint();
                            }
                            else if (code == BUTTON_O) {
                                stopAutoCountdown();
                                console.println("Exiting...");
                                try { finalCtx.notifyDestroyed(); } catch(Exception e){}
                                return;
                            }

                            Thread.sleep(12);
                        }

                    } catch(Throwable e) {
                        console.println("[EXCEPTION] " + e.toString());
                    }
                }
            }).start();

        } catch(Throwable e) {}
        scene.validate();
    }

    public void startXlet() throws XletStateChangeException {
        gui.setVisible(true);
        scene.setVisible(true);
        gui.requestFocus();
    }

    public void pauseXlet() {
        gui.setVisible(false);
    }

    public void destroyXlet(boolean unconditional) throws XletStateChangeException {
        try {
            if (scene != null && gui != null)
                scene.remove(gui);
        } catch(Exception e){}
        scene = null;
        gui = null;
        System.gc();
    }

    private void toggleAutoLoader() {
        autoLoadEnabled = !autoLoadEnabled;
        AutoLoadConfig.saveAutoLoadSetting(autoLoadEnabled);
        gui.setAutoLoadEnabled(autoLoadEnabled);
        console.println("Auto-Loader: " + (autoLoadEnabled ? "ON" : "OFF"));
        if (autoLoadEnabled && autoMode != -1) startAutoCountdown();
        else stopAutoCountdown();
    }

    private void startAutoCountdown() {
        countdown = 2;
        gui.setCountdown(countdown);

        autoTimer = new Timer();
        autoTimer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                countdown--;
                gui.setCountdown(countdown);
                if (countdown <= 0) {
                    stopAutoCountdown();
                    runSelection(autoMode);
                }
            }
        }, 1000, 1000);
    }

    private void resetAutoCountdown() {
        if (!autoLoadEnabled || autoMode == -1) return;
        countdown = 1;
        gui.setCountdown(countdown);
    }

    private void stopAutoCountdown() {
        if (autoTimer != null)
            autoTimer.cancel();
        autoTimer = null;
    }

    private void runSelection(int mode) {
        int r = -1;

        if (mode == 0) r = Lapse.main(console);
        else if (mode == 1) r = Poops.main(console);

        if (r == 0) console.println("Success !");
        else console.println("Fail (" + r + "), reboot PS4 !");
    }

    public void userEventReceived(UserEvent evt) {
        if (evt.getType() == HRcEvent.KEY_PRESSED)
            eq.put(new Integer(evt.getCode()));
    }

    public static int pollInput() {
        Object o = instance.eq.get();
        if (o == null) return 0;
        return ((Integer)o).intValue();
    }

    public static void repaint() {
        instance.scene.repaint();
    }
}
