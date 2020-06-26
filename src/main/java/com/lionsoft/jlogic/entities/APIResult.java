package com.lionsoft.jlogic;

import org.springframework.http.HttpStatus;

public class APIResult {

    int code = 0;
    HttpStatus status = HttpStatus.OK;
    String message = "";
    String response = "";

    public APIResult() {
    }

    public int getCode() {
      return (code);
    }

    public void setCode(int c) {
      this.code = c;
    }

    public HttpStatus getStatus() {
      return (status);
    }

    public void setStatus(HttpStatus s) {
      this.status = s;
    }

    public String getResponse() {
      return (response);
    }

    public void setResponse(String s) {
      this.response = s;
    }

    public String getMessage() {
      return (message);
    }

    public void setMessage(String m) {
      this.message = m;
    }

    public void setResult(int code, HttpStatus status, String message) {
      setCode(code);
      setStatus(status);
      setMessage(message);
    }

}
