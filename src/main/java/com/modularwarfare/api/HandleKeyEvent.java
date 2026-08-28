package com.modularwarfare.api;

import com.modularwarfare.client.input.KeyType;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

@Cancelable
public class HandleKeyEvent extends Event {

    public KeyType keyType;
    public HandleKeyEvent(KeyType keyType) {
        this.keyType = keyType;
    }
}
