package com.fulfilment.application.monolith.exceptions;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class BusinessRuleExceptionMapper implements ExceptionMapper<BusinessRuleException> {

  @Override
  public Response toResponse(BusinessRuleException exception) {
    return Response.status(Response.Status.BAD_REQUEST)
        .entity(new ErrorResponse(400, exception.getMessage()))
        .build();
  }
}
