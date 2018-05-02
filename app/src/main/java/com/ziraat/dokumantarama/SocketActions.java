package com.ziraat.dokumantarama;

/**
 * Created by bhdrgl on 20/11/2017.
 */

public enum SocketActions {

    CLOSE_APP(1),
    CAPTURE_PICTURE(2);

    private int value;

    private SocketActions(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static SocketActions fromKey(int val) {
        for (SocketActions type : SocketActions.values()) {
            if (type.getValue() == val) {
                return type;
            }
        }
        return null;
    }
}
