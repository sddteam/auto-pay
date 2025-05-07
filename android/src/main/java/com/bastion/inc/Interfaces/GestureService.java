package com.bastion.inc.Interfaces;

import com.bastion.inc.Enums.NodeInfoAttribute;

public interface GestureService extends AutoCloseable{
    void click(NodeInfoAttribute attribute, String text);

    void find(String text);

    void qr(String filename);
}
