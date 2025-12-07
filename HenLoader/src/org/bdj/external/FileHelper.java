package org.bdj.external;

import java.io.*;
import org.bdj.api.*;

public class FileHelper {

    private static final String SRC = "/disc/BDMV/AUXDATA/payload.bin";
    private static final String DST = "/data/payload.bin";

    public static void copyPayload(PrintStream console) {
        try {
            console.println("Checking payload...");

            File dst = new File(DST);
            if (dst.exists() && dst.length() > 0) {
                console.println("/data/payload.bin already exists !");
                return;
            }

            console.println("payload.bin missing : copying...");

            File src = new File(SRC);
            if (!src.exists()) {
                console.println("[ERR] Source payload not found : " + SRC);
                return;
            }

            copyFile(src, dst);

            if (dst.exists()) {
                console.println("Copy finished successfully !");
            } else {
                console.println("[ERR] Copy failed.");
            }

        } catch (Throwable e) {
            console.println("[EXCEPTION] " + e.toString());
        }
    }

    private static void copyFile(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);

        File parent = dst.getParentFile();
        if (!parent.exists()) parent.mkdirs();

        OutputStream out = new FileOutputStream(dst);

        byte[] buffer = new byte[8192];
        int len;

        while ((len = in.read(buffer)) > 0) {
            out.write(buffer, 0, len);
        }

        in.close();
        out.close();
    }
}
