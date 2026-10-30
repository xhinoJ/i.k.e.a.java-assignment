package com.fulfilment.application.monolith.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/**
 * Single error mapper for the whole application.
 *
 * <p>Keeping one provider avoids two {@code ExceptionMapper<Exception>} beans competing for the same
 * exception type. The JSON shape is unchanged from the original resource-level mappers:
 * {@code exceptionType}, {@code code} and, when present, {@code error}.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

  private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class);

  @Inject ObjectMapper objectMapper;

  @Override
  public Response toResponse(Exception exception) {
    int status =
        exception instanceof WebApplicationException webException
            ? webException.getResponse().getStatus()
            : 500;

    if (status >= 500) {
      LOGGER.errorf(exception, "Request failed with status %d", status);
    } else {
      // 4xx are expected business outcomes, not application failures.
      LOGGER.warnf("Request rejected with status %d: %s", status, exception.getMessage());
    }

    ObjectNode error = objectMapper.createObjectNode();
    error.put("exceptionType", exception.getClass().getName());
    error.put("code", status);

    if (exception.getMessage() != null) {
      error.put("error", exception.getMessage());
    }

    return Response.status(status).entity(error).build();
  }
}
