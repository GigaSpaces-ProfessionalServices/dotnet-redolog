package com.mycompany.app.model;
import com.gigaspaces.client.*;
import com.gigaspaces.server.*;

public class MultiplyIntegerChangeOperation extends CustomChangeOperation {
    private static final long serialVersionUID = 1L;
    private final String path;
    private final int multiplier;

    public MultiplyIntegerChangeOperation(String path, int multiplier) {
        this.path = path;
        this.multiplier = multiplier;
    }

    @Override
    public String getName() {
        return "multiplyInt";
    }

    public String getPath() {
        return path;
    }

    public int getMultiplier() {
        return multiplier;
    }

    @Override
    public Object change(MutableServerEntry entry) {
        //Assume this is an integer property, if this is not true an exception will be thrown
        //and the change operation will fail
        int oldValue = (Integer)entry.getPathValue(path);
        int newValue = oldValue * multiplier;
        entry.setPathValue(path, newValue);
        return newValue;
    }
}