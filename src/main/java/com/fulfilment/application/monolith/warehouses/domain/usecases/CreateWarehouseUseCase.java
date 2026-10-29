package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public CreateWarehouseUseCase(
      WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  @Transactional
  public void create(Warehouse warehouse) {
    WarehouseValidation.validateBasics(warehouse);

    if (warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode) != null) {
      throw new WebApplicationException(
          "A warehouse with business unit code "
              + warehouse.businessUnitCode
              + " already exists.",
          422);
    }

    Location location = locationResolver.resolveByIdentifier(warehouse.location);
    if (location == null) {
      throw new WebApplicationException(
          "Location " + warehouse.location + " is not a valid location.", 422);
    }

    // Store the canonical identifier so subsequent capacity/count queries stay consistent.
    warehouse.location = location.identification;

    long warehousesAtLocation = warehouseStore.countByLocation(location.identification);
    if (warehousesAtLocation >= location.maxNumberOfWarehouses) {
      throw new WebApplicationException(
          "Location "
              + location.identification
              + " already has the maximum number of warehouses ("
              + location.maxNumberOfWarehouses
              + ").",
          422);
    }

    long capacityAtLocation = warehouseStore.sumCapacityByLocation(location.identification);
    if (capacityAtLocation + warehouse.capacity > location.maxCapacity) {
      throw new WebApplicationException(
          "Warehouse capacity of "
              + warehouse.capacity
              + " exceeds the remaining capacity of location "
              + location.identification
              + " (maximum "
              + location.maxCapacity
              + ", already used "
              + capacityAtLocation
              + ").",
          422);
    }

    warehouseStore.create(warehouse);
  }
}
