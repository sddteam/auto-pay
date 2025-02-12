package com.bastion.inc;

public interface GestureService extends AutoCloseable{
    void click(Location location, int times);

    void find(String text);

    default void click(Location location){
        click(location, 1);
    }

    void ads();

    void qr(String filename);
}
