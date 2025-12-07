package org.bdj.external;

import java.io.*;
import java.util.ArrayList;

import org.bdj.InitXlet;
import org.bdj.api.*;

public class HomebrewLauncher {

    private static final String PAYLOAD_DIR = "/disc/extras/payloads";

    public static void openMenu(PrintStream console) {

        synchronized(InitXlet.messages) {
            InitXlet.messages.clear();
        }
        InitXlet.repaint();

        console.println("Homebrew / Payload Launcher");
        console.println("Scanning directory: " + PAYLOAD_DIR);

        File dir = new File(PAYLOAD_DIR);
        if (!dir.exists() || !dir.isDirectory()) {
            console.println("[ERR] Directory not found or not a directory: " + PAYLOAD_DIR);
            return;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            console.println("[ERR] Unable to list files in: " + PAYLOAD_DIR);
            return;
        }

        ArrayList payloads = new ArrayList();
        int i;

        for (i = 0; i < files.length; i++) {
            File f = files[i];
            if (f.isFile()) {
                String name = f.getName();
                String lower = name.toLowerCase();
                if (lower.endsWith(".bin")) {
                    payloads.add(f);
                }
            }
        }

        if (payloads.size() == 0) {
            console.println("[INFO] No .bin payloads found in " + PAYLOAD_DIR);
            return;
        }

        console.println("[INFO] Found " + payloads.size() + " payload(s).");
        console.println("Use UP/DOWN to choose, X to launch, O to go back.");

        int index = 0;

        while (true) {

            synchronized(InitXlet.messages) {
                InitXlet.messages.clear();
                InitXlet.messages.add("Homebrew / Payload Launcher:");

                for (i = 0; i < payloads.size(); i++) {
                    File f = (File) payloads.get(i);
                    String prefix = (i == index) ? " > " : "   ";
                    InitXlet.messages.add(prefix + f.getName());
                }
            }

            InitXlet.repaint();

            int code = 0;
            while (code == 0) {
                code = InitXlet.pollInput();
                try { Thread.sleep(20); } catch (Exception e) {}
            }

            if (code == InitXlet.BUTTON_U) {
                index--;
                if (index < 0) index = payloads.size() - 1;
            }
            else if (code == InitXlet.BUTTON_D) {
                index++;
                if (index >= payloads.size()) index = 0;
            }
            else if (code == InitXlet.BUTTON_O) {
                return;
            }
            else if (code == InitXlet.BUTTON_X) {
                File selected = (File) payloads.get(index);
                runPayload(console, selected);
            }
        }
    }

    private static void runPayload(PrintStream console, File f) {
        console.println("");
        console.println("Loading payload: " + f.getName());

        if (!f.exists()) {
            console.println("[ERR] File not found: " + f.getAbsolutePath());
            return;
        }

        try {
            FileInputStream fi = new FileInputStream(f);
            byte[] data = new byte[fi.available()];
            int read = fi.read(data);
            fi.close();

            if (read <= 0) {
                console.println("[ERR] Failed to read data from " + f.getName());
                return;
            }

            console.println("BIN loaded (" + read + " bytes)");
            console.println("Mapping and Executing...");

            BinLoader.loadFromData(data);
            BinLoader.run();
            BinLoader.waitForPayloadToExit();

            console.println("Payload finished: " + f.getName());
        } catch (Throwable e) {
            console.println("[EXCEPTION] " + e.toString());
        }
    }
}
