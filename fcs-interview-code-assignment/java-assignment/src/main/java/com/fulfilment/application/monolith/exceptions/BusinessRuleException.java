package com.fulfilment.application.monolith.exceptions;

/** Raised when a request is well formed but violates one of the business constraints. */
public class BusinessRuleException extends RuntimeException {

  public BusinessRuleException(String message) {
    super(message);
  }
}
