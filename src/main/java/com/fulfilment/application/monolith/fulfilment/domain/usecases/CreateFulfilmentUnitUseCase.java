package com.fulfilment.application.monolith.fulfilment.domain.usecases;

import com.fulfilment.application.monolith.fulfilment.domain.models.FulfilmentUnit;
import com.fulfilment.application.monolith.fulfilment.domain.ports.CreateFulfilmentUnitOperation;
import com.fulfilment.application.monolith.fulfilment.domain.ports.FulfilmentUnitStore;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.StoreRepository;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CreateFulfilmentUnitUseCase implements CreateFulfilmentUnitOperation {

  private static final Logger LOGGER = Logger.getLogger(CreateFulfilmentUnitUseCase.class);

  static final int MAX_WAREHOUSES_PER_PRODUCT_PER_STORE = 2;
  static final int MAX_WAREHOUSES_PER_STORE = 3;
  static final int MAX_PRODUCT_TYPES_PER_WAREHOUSE = 5;

  private final FulfilmentUnitStore fulfilmentUnitStore;
  private final WarehouseStore warehouseStore;
  private final StoreRepository storeRepository;
  private final ProductRepository productRepository;

  public CreateFulfilmentUnitUseCase(
      FulfilmentUnitStore fulfilmentUnitStore,
      WarehouseStore warehouseStore,
      StoreRepository storeRepository,
      ProductRepository productRepository) {
    this.fulfilmentUnitStore = fulfilmentUnitStore;
    this.warehouseStore = warehouseStore;
    this.storeRepository = storeRepository;
    this.productRepository = productRepository;
  }

  @Override
  @Transactional
  public FulfilmentUnit create(FulfilmentUnit unit) {
    validateIds(unit);

    if (storeRepository.findById(unit.storeId) == null) {
      throw new WebApplicationException(
          "Store with id of " + unit.storeId + " does not exist.", 404);
    }

    if (productRepository.findById(unit.productId) == null) {
      throw new WebApplicationException(
          "Product with id of " + unit.productId + " does not exist.", 404);
    }

    if (warehouseStore.findWarehouseById(unit.warehouseId) == null) {
      throw new WebApplicationException(
          "Warehouse with id of " + unit.warehouseId + " does not exist.", 404);
    }

    if (fulfilmentUnitStore.exists(unit.storeId, unit.productId, unit.warehouseId)) {
      throw new WebApplicationException(
          "This warehouse is already associated with the product for the store.", 422);
    }

    long warehousesForProductInStore =
        fulfilmentUnitStore.countDistinctWarehousesByStoreAndProduct(unit.storeId, unit.productId);
    if (warehousesForProductInStore >= MAX_WAREHOUSES_PER_PRODUCT_PER_STORE) {
      throw new WebApplicationException(
          "A product can be fulfilled by at most "
              + MAX_WAREHOUSES_PER_PRODUCT_PER_STORE
              + " warehouses per store.",
          422);
    }

    // Reusing a warehouse already linked to the store does not increase the store's warehouse
    // count, so the limit only applies when the warehouse is new to the store.
    boolean warehouseAlreadyUsedByStore =
        fulfilmentUnitStore.existsByStoreAndWarehouse(unit.storeId, unit.warehouseId);
    if (!warehouseAlreadyUsedByStore) {
      long warehousesForStore = fulfilmentUnitStore.countDistinctWarehousesByStore(unit.storeId);
      if (warehousesForStore >= MAX_WAREHOUSES_PER_STORE) {
        throw new WebApplicationException(
            "A store can be fulfilled by at most "
                + MAX_WAREHOUSES_PER_STORE
                + " warehouses.",
            422);
      }
    }

    // Reusing a product already stored by the warehouse does not increase the warehouse's product
    // type count, so the limit only applies when the product is new to the warehouse.
    boolean productAlreadyStoredByWarehouse =
        fulfilmentUnitStore.existsByWarehouseAndProduct(unit.warehouseId, unit.productId);
    if (!productAlreadyStoredByWarehouse) {
      long productTypesForWarehouse =
          fulfilmentUnitStore.countDistinctProductsByWarehouse(unit.warehouseId);
      if (productTypesForWarehouse >= MAX_PRODUCT_TYPES_PER_WAREHOUSE) {
        throw new WebApplicationException(
            "A warehouse can store at most "
                + MAX_PRODUCT_TYPES_PER_WAREHOUSE
                + " product types.",
            422);
      }
    }

    // The database assigns the identifier; never trust one supplied by the client.
    unit.id = null;
    FulfilmentUnit created = fulfilmentUnitStore.create(unit);

    LOGGER.infof(
        "Created fulfilment unit id=%d storeId=%d productId=%d warehouseId=%d",
        created.id, created.storeId, created.productId, created.warehouseId);

    return created;
  }

  private void validateIds(FulfilmentUnit unit) {
    if (unit == null) {
      throw new WebApplicationException("Fulfilment unit data was not provided.", 422);
    }
    if (unit.storeId == null) {
      throw new WebApplicationException("Store id was not set on request.", 422);
    }
    if (unit.productId == null) {
      throw new WebApplicationException("Product id was not set on request.", 422);
    }
    if (unit.warehouseId == null) {
      throw new WebApplicationException("Warehouse id was not set on request.", 422);
    }
  }
}
