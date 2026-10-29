package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import jakarta.ws.rs.WebApplicationException;

/**
 * Basic payload validations shared by the warehouse use cases.
 *
 * <p>Business rules that depend on persisted data (uniqueness, location feasibility, stock matching)
 * are handled by the individual use cases.
 */
final class WarehouseValidation {

  private WarehouseValidation() {}

  static void validateBasics(Warehouse warehouse) {
    if (warehouse == null) {
      throw new WebApplicationException("Warehouse data was not provided.", 422);
    }

    if (warehouse.businessUnitCode == null || warehouse.businessUnitCode.isBlank()) {
      throw new WebApplicationException("Business Unit Code was not set on request.", 422);
    }

    if (warehouse.location == null || warehouse.location.isBlank()) {
      throw new WebApplicationException("Warehouse location was not set on request.", 422);
    }

    if (warehouse.capacity == null) {
      throw new WebApplicationException("Warehouse capacity was not set on request.", 422);
    }

    if (warehouse.stock == null) {
      throw new WebApplicationException("Warehouse stock was not set on request.", 422);
    }

    if (warehouse.capacity <= 0) {
      throw new WebApplicationException("Warehouse capacity must be greater than zero.", 422);
    }

    if (warehouse.stock < 0) {
      throw new WebApplicationException("Warehouse stock cannot be negative.", 422);
    }

    if (warehouse.stock > warehouse.capacity) {
      throw new WebApplicationException(
          "Warehouse stock ("
              + warehouse.stock
              + ") cannot exceed its capacity ("
              + warehouse.capacity
              + ").",
          422);
    }
  }
}
