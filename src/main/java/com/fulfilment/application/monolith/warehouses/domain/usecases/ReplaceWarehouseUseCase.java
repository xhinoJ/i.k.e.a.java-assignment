package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public ReplaceWarehouseUseCase(
      WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  @Transactional
  public void replace(Warehouse newWarehouse) {
    WarehouseValidation.validateBasics(newWarehouse);

    Warehouse previous = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (previous == null) {
      throw new WebApplicationException(
          "Warehouse with business unit code "
              + newWarehouse.businessUnitCode
              + " does not exist.",
          404);
    }

    Location location = locationResolver.resolveByIdentifier(newWarehouse.location);
    if (location == null) {
      throw new WebApplicationException(
          "Location " + newWarehouse.location + " is not a valid location.", 422);
    }

    // Store the canonical identifier so subsequent capacity/count queries stay consistent.
    newWarehouse.location = location.identification;

    int previousStock = previous.stock == null ? 0 : previous.stock;

    if (newWarehouse.capacity < previousStock) {
      throw new WebApplicationException(
          "New warehouse capacity ("
              + newWarehouse.capacity
              + ") cannot accommodate the previous stock ("
              + previousStock
              + ").",
          422);
    }

    if (newWarehouse.stock != previousStock) {
      throw new WebApplicationException(
          "New warehouse stock ("
              + newWarehouse.stock
              + ") must match the previous stock ("
              + previousStock
              + ").",
          422);
    }

    // A replacement archives the current warehouse and creates a new one with the same business
    // unit code. Both operations happen in the same transaction.
    warehouseStore.remove(previous);
    warehouseStore.create(newWarehouse);
  }
}
