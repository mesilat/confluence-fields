package com.mesilat.confield;

public class DataServiceResult {
    private final int status;
    private final String text;

    public int getStatus(){
        return status;
    }
    public String getText(){
        return text;
    }

    public DataServiceResult(int status, String text){
        this.status = status;
        this.text = text;
    }
}