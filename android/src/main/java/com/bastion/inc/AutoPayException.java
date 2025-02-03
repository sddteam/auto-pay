package com.bastion.inc;

public class AutoPayException extends Exception{
    private final int code;

    public AutoPayException(int code){
        super(String.valueOf(code));
        this.code = code;
    }

    public String getErrorCode(){
        return String.valueOf(code);
    }
}
