package org.bdj.external;

import java.io.*;
import org.bdj.api.*;

public class TestBinRunner {

    private static final String KERNEL_DUMPER_PATH = "/disc/BDMV/AUXDATA/ps4-kernel-dumper.bin";
    private static final String DISABLE_UPDATES_PATH = "/disc/BDMV/AUXDATA/ps4-disable-updates.bin";

    public static void runTestBin(PrintStream console) {
        runBin(console, KERNEL_DUMPER_PATH, "Kernel Dumper BIN finished.");
    }

    public static void runDisableUpdates(PrintStream console) {
        runBin(console, DISABLE_UPDATES_PATH, "Disable Updates BIN finished.");
    }

    private static void runBin(PrintStream console, String path, String doneMsg) {
        console.println("Loading BIN: " + path);
        File f = new File(path);

        if (!f.exists()) {
            console.println("[ERR] File not found : " + path);
            return;
        }

        try {
            FileInputStream fi = new FileInputStream(f);
            byte[] data = new byte[fi.available()];
            int read = fi.read(data);
            fi.close();

            if (read <= 0) {
                console.println("[ERR] Failed to read data");
                return;
            }

            console.println("BIN loaded (" + read + " bytes)");
            console.println("Mapping and Executing...");

            BinLoader.loadFromData(data);
            BinLoader.run();
            BinLoader.waitForPayloadToExit();

            console.println(doneMsg);

        } catch (Throwable e) {
            console.println("[EXCEPTION] " + e.toString());
        }
    }
}
