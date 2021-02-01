package com.lionsoft.jlogic;

public class Result {
    static final int SUCCESS = 0;
    static final int ERROR = 1;

    int code;
    String message;
    String output;
    String stdout, stderr;
    Object data;

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

    public String getStdErr() { return stderr; }
    public void setStdErr(String stderr) { this.stderr = stderr; }

    public String getStdOut() { return stdout; }
    public void setStdOut(String stdout) { this.stdout = stdout; }

    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }

    public Result setResult(int code, String message) {
        setCode(code);
        setMessage(message);
        return(this);
    }

    public Result setError(String message) {
        setResult(ERROR, message);
        return(this);
    }

    public boolean success() { return(code == SUCCESS); }
}
