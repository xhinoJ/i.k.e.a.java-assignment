package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private static final Logger LOGGER = Logger.getLogger(ReplaceWarehouseUseCase.class);

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

    validateLocationFeasibility(previous, newWarehouse, location);

    // A replacement archives the current warehouse and creates a new one with the same business
    // unit code. Both operations happen in the same transaction.
    warehouseStore.remove(previous);
    warehouseStore.create(newWarehouse);

    LOGGER.infof(
        "Replaced warehouse businessUnitCode=%s location=%s capacity=%d stock=%d",
        newWarehouse.businessUnitCode,
        newWarehouse.location,
        newWarehouse.capacity,
        newWarehouse.stock);
  }

  /**
   * Applies the location feasibility rules to a replacement.
   *
   * <p>A replacement that keeps the warehouse in the same location only changes the capacity, so
   * the previous capacity is subtracted before adding the new one. Moving to a different location
   * is a genuine addition and must respect that location's warehouse count and capacity limits.
   */
  private void validateLocationFeasibility(
      Warehouse previous, Warehouse newWarehouse, Location location) {
    long usedCapacity = warehouseStore.sumCapacityByLocation(location.identification);
    int previousCapacity = previous.capacity == null ? 0 : previous.capacity;

    boolean sameLocation =
        previous.location != null && previous.location.equalsIgnoreCase(location.identification);

    if (!sameLocation) {
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
    }

    long capacityAfterReplacement =
        sameLocation
            ? usedCapacity - previousCapacity + newWarehouse.capacity
            : usedCapacity + newWarehouse.capacity;

    if (capacityAfterReplacement > location.maxCapacity) {
      throw new WebApplicationException(
          "Warehouse capacity of "
              + newWarehouse.capacity
              + " exceeds the remaining capacity of location "
              + location.identification
              + " (maximum "
              + location.maxCapacity
              + ", already used "
              + (usedCapacity - (sameLocation ? previousCapacity : 0))
              + ").",
          422);
    }
  }
}
