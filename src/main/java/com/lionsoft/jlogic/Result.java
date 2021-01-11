package com.lionsoft.jlogic;

public class Result {
    static final int SUCCESS = 0;
    static final int ERROR = 1;

    int code;
    String message;
    String output;

    public Result() {
        code = SUCCESS;
        message = "Success";
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }

    public void setResult(int code, String message) {
        setCode(code);
        setMessage(message);
    }

    public boolean success() { return(code == SUCCESS); }
}