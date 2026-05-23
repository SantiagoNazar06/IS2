package com.is1.proyecto.exceptions;

public class ValidationException extends RuntimeException{
    private String details;
        
    public ValidationException(String message, String details) {
        super(message);
        this.details = details;
    }

    public String getDetails(){
        return details;
    }
}
