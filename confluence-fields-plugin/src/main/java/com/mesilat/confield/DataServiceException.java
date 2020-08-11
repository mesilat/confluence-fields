package com.mesilat.confield;

import javax.ws.rs.core.Response.Status;

public class DataServiceException extends Exception {
    private final Status status;
    private final String header;

    public Status getStatus(){
        return status;
    }
    public String getHeader(){
        return header;
    }

    public DataServiceException(Status status, String message){
        super(message);
        this.status = status;
        this.header = null;
    }
    public DataServiceException(Status status, String message, Throwable cause){
        super(message, cause);
        this.status = status;
        this.header = null;
    }
    public DataServiceException(Status status, String message, Throwable cause, String header){
        super(message, cause);
        this.status = status;
        this.header = header;
    }
}