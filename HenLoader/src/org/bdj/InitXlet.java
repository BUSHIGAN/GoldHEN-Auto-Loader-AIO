package org.bdj;

import java.io.*;
import java.util.*;
import javax.tv.xlet.*;
import java.awt.BorderLayout;

import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;

import org.dvb.event.UserEvent;
import org.dvb.event.EventManager;
import org.dvb.event.UserEventListener;
import org.dvb.event.UserEventRepository;

import org.bluray.ui.event.HRcEvent;

import org.bdj.sandbox.DisableSecurityManagerAction;
import org.bdj.external.*;

public class InitXlet implements Xlet, UserEventListener
{
    public static final int BUTTON_X = 10;
    public static final int BUTTON_O = 19;
    public static final int BUTTON_U = 38;
    public static final int BUTTON_D = 40;

    public static InitXlet instance;

    public static class EventQueue
    {
        private LinkedList l;
        int cnt = 0;

        EventQueue()
        {
            l = new LinkedList();
        }

        public synchronized void put(Object obj)
        {
            l.addLast(obj);
            cnt++;
        }

        public synchronized Object get()
        {
            if(cnt == 0)
                return null;
            Object o = l.getFirst();
            l.removeFirst();
            cnt--;
            return o;
        }
    }

    private EventQueue eq;
    private HScene scene;
    private Screen gui;
    public XletContext context;
    private static PrintStream console;
    private static final ArrayList messages = new ArrayList();

    private Timer autoTimer;
    private int countdown = 10;

    private boolean fwSupportsLapse = false;
    private boolean fwSupportsPoops = false;

    public void initXlet(XletContext context)
    {
        try { DisableSecurityManagerAction.execute(); } catch(Exception e) {}

        InitXlet.instance = this;
        this.context = context;
        this.eq = new EventQueue();
        scene = HSceneFactory.getInstance().getDefaultHScene();

        try
        {
            gui = new Screen(messages);
            gui.setSize(1920, 1080);
            scene.add(gui, BorderLayout.CENTER);

            UserEventRepository repo = new UserEventRepository("input");
            repo.addKey(BUTTON_X);
            repo.addKey(BUTTON_O);
            repo.addKey(BUTTON_U);
            repo.addKey(BUTTON_D);
            EventManager.getInstance().addUserEventListener(this, repo);

            new Thread()
            {
                public void run()
                {
                    try
                    {
                        scene.repaint();
                        console = new PrintStream(new MessagesOutputStream(messages, scene));

                        console.println("");
                        console.println("- GoldHEN 2.4b18.7 by SiSTR0");
                        console.println("- POOPS code by Theflow0");
                        console.println("- LAPSE code by Gezine");
                        console.println("- BD-JB build environment by Kimariin");
                        console.println("- JAVA console by Sleirsgoevy");
                        console.println("- BD-JB designed and modded by Bushigan");
                        console.println("");

                        System.gc();

                        Kernel.initializeKernelOffsets();
                        String fw = Helper.getCurrentFirmwareVersion();
                        gui.setFirmware(fw);
                        console.println("Firmware : " + fw);

                        if (!KernelOffset.hasPS4Offsets()) {
                            console.println("Unsupported Firmware !");
                            return;
                        }

                        try {
                            float f = Float.parseFloat(fw);
                            if (f >= 9.00f && f <= 12.02f) fwSupportsLapse = true;
                            if (f >= 12.50f && f <= 12.52f) fwSupportsPoops = true;
                        } catch(Exception e){}

                        fwSupportsPoops = !fwSupportsLapse || fwSupportsPoops;

                        if (fwSupportsLapse) {
                            console.println("AUTO-SELECT: LAPSE");
                            gui.moveSelection(-9999);
                        } else {
                            console.println("AUTO-SELECT: POOPS");
                            gui.moveSelection(9999);
                        }

                        console.println("Auto-loader armed (" +
                                       (fwSupportsLapse ? "LAPSE" : "POOPS") + ")");

                        startAutoCountdown();

                        while(true)
                        {
                            int code = pollInput();

                            if(code == BUTTON_U) {
                                gui.moveSelection(-1);
                                resetAutoCountdown();
                            }
                            else if(code == BUTTON_D) {
                                gui.moveSelection(1);
                                resetAutoCountdown();
                            }
                            else if(code == BUTTON_X) {
                                stopAutoCountdown();
                                int sel = gui.getSelected();
                                if (sel == 0) runSelection(true);
                                else if (sel == 1) runSelection(false);
                                break;
                            }
                            else if(code == BUTTON_O) {
                                stopAutoCountdown();
                                console.println("Exiting Auto-Loader…");
                                console.println("Goodbye from Loader.");
                                try {
                                    InitXlet.instance.context.notifyDestroyed();
                                } catch(Exception e){}
                                return;
                            }

                            try { Thread.sleep(10); }
                            catch(Exception e){}
                        }

                    }
                    catch(Throwable e)
                    {
                        scene.repaint();
                    }
                }
            }.start();

        }
        catch(Throwable e)
        {
            printStackTrace(e);
        }

        scene.validate();
    }

    private void startAutoCountdown()
    {
        countdown = 10;
        gui.setCountdown(countdown);

        autoTimer = new Timer();
        autoTimer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                countdown--;
                gui.setCountdown(countdown);

                if (countdown <= 0) {
                    stopAutoCountdown();
                    if (fwSupportsLapse) runSelection(true);
                    else runSelection(false);
                }
            }
        }, 1000, 1000);
    }

    private void resetAutoCountdown() {
        countdown = 10;
        gui.setCountdown(countdown);
    }

    private void stopAutoCountdown() {
        if(autoTimer != null)
            autoTimer.cancel();
        autoTimer = null;
    }

    private void runSelection(boolean isLapse)
    {
        int result;

        if (isLapse) {
            console.println("Running LAPSE...");
            result = org.bdj.external.Lapse.main(console);
        } else {
            console.println("Running POOPS...");
            result = org.bdj.external.Poops.main(console);
        }

        if(result == 0)
            console.println("Success !");
        else
            console.println("Fatal fail(" + result + "), please REBOOT PS4");
    }

    public void startXlet()
    {
        gui.setVisible(true);
        scene.setVisible(true);
        gui.requestFocus();
    }

    public void pauseXlet()
    {
        gui.setVisible(false);
    }

    public void destroyXlet(boolean unconditional)
    {
        try {
            if (scene != null && gui != null)
                scene.remove(gui);
        } catch(Exception e){}

        scene = null;
        gui = null;

        System.gc();
    }

    private void printStackTrace(Throwable e)
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        if(console != null)
            console.print(sw.toString());
    }

    public void userEventReceived(UserEvent evt)
    {
        if(evt.getType() == HRcEvent.KEY_PRESSED)
            eq.put(new Integer(evt.getCode()));
    }

    public static void repaint()
    {
        instance.scene.repaint();
    }

    public static int pollInput()
    {
        Object ans = instance.eq.get();
        if(ans == null)
            return 0;
        return ((Integer)ans).intValue();
    }
}
