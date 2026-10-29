package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * Enforces the {@code 201 Created} status documented by the Warehouse OpenAPI contract.
 *
 * <p>The generated JAX-RS interface declares the create operation with a {@code 201} response but
 * returns the warehouse bean directly. Since the JAX-RS annotations live on the generated interface,
 * RESTEasy Reactive does not pick up {@code @ResponseStatus} from the implementation method, so the
 * default {@code 200} would be returned without this filter.
 */
@Provider
public class WarehouseCreatedResponseFilter implements ContainerResponseFilter {

  @Override
  public void filter(
      ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    if ("POST".equalsIgnoreCase(requestContext.getMethod())
        && "warehouse".equals(trimSlashes(requestContext.getUriInfo().getPath()))
        && responseContext.getStatus() == 200) {
      responseContext.setStatus(201);
    }
  }

  private String trimSlashes(String path) {
    return path == null ? "" : path.replaceAll("^/+|/+$", "");
  }
}
