package org.bdj;

import java.io.OutputStream;
import java.util.ArrayList;
import org.havi.ui.HScene;

public class MessagesOutputStream extends OutputStream
{
    ArrayList messages;
    HScene scene;
    String cur;

    public MessagesOutputStream(ArrayList msgs, HScene sc)
    {
        messages = msgs;
        scene = sc;
        cur = "";
        messages.add(cur);
    }

    private void ensureLine()
    {
        if (messages.size() == 0) {
            cur = "";
            messages.add(cur);
        }
    }

    public synchronized void write(int c)
    {
        if (c == '\r') return;

        ensureLine();

        if (c == '\n')
        {
            messages.set(messages.size() - 1, cur);
            cur = "";
            messages.add(cur);
            scene.repaint();
            return;
        }

        cur += (char)c;
        messages.set(messages.size() - 1, cur);
    }
}
