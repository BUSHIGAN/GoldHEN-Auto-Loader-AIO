package org.bdj.external;

import java.io.*;
import java.util.ArrayList;

import org.bdj.InitXlet;
import org.bdj.api.*;

public class UsbPayloadLauncher {

    private static final String[] USB_PATHS = {
        "/mnt/usb0/PS4",
        "/mnt/usb1/PS4",
        "/mnt/usb2/PS4",
        "/mnt/usb3/PS4",
        "/mnt/usb4/PS4",
        "/mnt/usb5/PS4",
        "/mnt/usb6/PS4",
        "/mnt/usb7/PS4"
    };

    public static void openUSB(PrintStream console) {

        synchronized(InitXlet.messages) {
            InitXlet.messages.clear();
        }
        InitXlet.repaint();

        console.println("USB Payload Browser");
        console.println("Scanning USB devices...");

        ArrayList files = new ArrayList();

        int i;
        int j;

        for (i = 0; i < USB_PATHS.length; i++) {
            File d = new File(USB_PATHS[i]);
            if (d.exists() && d.isDirectory()) {

                File[] list = d.listFiles();
                if (list != null) {

                    for (j = 0; j < list.length; j++) {
                        File f = list[j];
                        if (f.isFile()) {

                            String n = f.getName().toLowerCase();
                            if (n.endsWith(".bin")) {
                                files.add(f);
                            }
                        }
                    }
                }
            }
        }

        if (files.size() == 0) {
            console.println("No payloads found on USB.");
            return;
        }

        console.println("Found " + files.size() + " payload(s). Use UP/DOWN, X to run, O to exit.");

        int index = 0;

        while (true) {

            synchronized(InitXlet.messages) {

                InitXlet.messages.clear();
                InitXlet.messages.add("USB Payload Browser:");

                for (i = 0; i < files.size(); i++) {
                    File f = (File) files.get(i);
                    String mark = (i == index) ? " > " : "   ";
                    InitXlet.messages.add(mark + f.getName());
                }
            }

            InitXlet.repaint();

            int code = 0;

            while (code == 0) {
                code = InitXlet.pollInput();
                try { Thread.sleep(20); } catch(Exception e) {}
            }

            if (code == InitXlet.BUTTON_U) {
                index--;
                if (index < 0) index = files.size() - 1;
            }
            else if (code == InitXlet.BUTTON_D) {
                index++;
                if (index >= files.size()) index = 0;
            }
            else if (code == InitXlet.BUTTON_O) {
                return;
            }
            else if (code == InitXlet.BUTTON_X) {
                File f = (File) files.get(index);
                launch(console, f);
            }
        }
    }

    private static void launch(PrintStream console, File f) {

        console.println("Loading: " + f.getName());

        if (!f.exists()) {
            console.println("File not found.");
            return;
        }

        try {
            FileInputStream fi = new FileInputStream(f);
            byte[] data = new byte[fi.available()];
            int r = fi.read(data);
            fi.close();

            if (r <= 0) {
                console.println("Read error.");
                return;
            }

            console.println("Executing payload...");
            BinLoader.loadFromData(data);
            BinLoader.run();
            BinLoader.waitForPayloadToExit();
            console.println("Done.");

        } catch (Throwable e) {
            console.println("Exception: " + e.toString());
        }
    }
}
