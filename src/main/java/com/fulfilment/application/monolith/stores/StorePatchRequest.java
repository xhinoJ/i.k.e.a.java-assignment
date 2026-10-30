package com.fulfilment.application.monolith.stores;

/**
 * Request payload for {@code PATCH /store/{id}}.
 *
 * <p>Stock is deliberately {@link Integer} rather than {@code int}: {@code null} means "field not
 * provided", which is different from an explicit {@code 0}. Using the {@link Store} entity directly
 * would collapse both cases, because Jackson leaves a missing primitive at its default value.
 */
public class StorePatchRequest {

  public String name;

  public Integer quantityProductsInStock;
}
