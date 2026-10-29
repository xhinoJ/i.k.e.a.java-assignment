package com.fulfilment.application.monolith.fulfilment.domain.models;

/** Association between a warehouse, a product and the store it fulfils the product for. */
public class FulfilmentUnit {

  public Long id;

  public Long storeId;

  public Long productId;

  public Long warehouseId;
}
