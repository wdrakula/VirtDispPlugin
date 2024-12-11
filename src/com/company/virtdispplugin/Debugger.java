package com.company.virtdispplugin;

public class Debugger {

    Debugger(boolean en) {
        enable = en;
    }

    private final boolean enable;
    public  boolean isEnabled(){
        return enable;
    }

    public void log(Object o){
        if(isEnabled()) {
            System.out.println(o.toString());
        }
    }


}
