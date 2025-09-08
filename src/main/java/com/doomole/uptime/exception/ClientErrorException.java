package com.doomole.uptime.exception;

public class ClientErrorException extends RuntimeException {
    public ClientErrorException(String message) { super(message); }
}
