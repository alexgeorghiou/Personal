package com.fulfilment.application.monolith.exceptions;

public class ErrorResponse {

  public int code;

  public String error;

  public ErrorResponse() {}

  public ErrorResponse(int code, String error) {
    this.code = code;
    this.error = error;
  }
}
