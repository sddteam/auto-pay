package com.bastion.inc;

public interface GestureService extends AutoCloseable{
    void click(Location location, int times, ActionState state);

    void find(String text);

    default void click(Location location, ActionState state){
        click(location, 1, state);
    }

    void ads();

    void qr(String filename);
}
