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
 *
 * <p>Database constraint violations are translated to {@code 409 Conflict} instead of leaking as
 * {@code 500}. Server-side failures never echo the internal exception message, which can contain SQL
 * and schema details.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

  private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class);

  private static final String CONFLICT_MESSAGE =
      "Request conflicts with the current state of the resource.";
  private static final String CONSTRAINT_VIOLATION_TYPE = "ConstraintViolationException";
  private static final String GENERIC_SERVER_ERROR = "Internal server error";

  @Inject ObjectMapper objectMapper;

  @Override
  public Response toResponse(Exception exception) {
    int status =
        exception instanceof WebApplicationException webException
            ? webException.getResponse().getStatus()
            : 500;

    String exceptionType = exception.getClass().getName();
    String message = exception.getMessage();

    if (status >= 500 && isConstraintViolation(exception)) {
      status = 409;
      exceptionType = CONSTRAINT_VIOLATION_TYPE;
      message = CONFLICT_MESSAGE;
      LOGGER.warnf("Request rejected with status %d: constraint violation", status);
    } else if (status >= 500) {
      // Never expose SQL, constraint names or other internals to the caller.
      message = GENERIC_SERVER_ERROR;
      LOGGER.errorf(exception, "Request failed with status %d", status);
    } else {
      // 4xx are expected business outcomes, not application failures.
      LOGGER.warnf("Request rejected with status %d: %s", status, message);
    }

    ObjectNode error = objectMapper.createObjectNode();
    error.put("exceptionType", exceptionType);
    error.put("code", status);

    if (message != null) {
      error.put("error", message);
    }

    return Response.status(status).entity(error).build();
  }

  /**
   * Walks the cause chain because transactional failures are wrapped: a unique-key violation can
   * surface as an Arc proxy exception wrapping a {@code RollbackException} wrapping Hibernate's
   * {@code ConstraintViolationException}.
   */
  private boolean isConstraintViolation(Throwable throwable) {
    Throwable current = throwable;
    for (int depth = 0; current != null && depth < 16; depth++) {
      if (current instanceof org.hibernate.exception.ConstraintViolationException) {
        return true;
      }
      if (current instanceof java.sql.SQLException sqlException
          && sqlException.getSQLState() != null
          && sqlException.getSQLState().startsWith("23")) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }
}
