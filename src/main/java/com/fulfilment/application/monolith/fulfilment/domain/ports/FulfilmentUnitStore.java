package com.fulfilment.application.monolith.fulfilment.domain.ports;

import com.fulfilment.application.monolith.fulfilment.domain.models.FulfilmentUnit;
import java.util.List;

public interface FulfilmentUnitStore {

  List<FulfilmentUnit> getAll();

  FulfilmentUnit findFulfilmentUnitById(Long id);

  FulfilmentUnit create(FulfilmentUnit unit);

  void remove(FulfilmentUnit unit);

  boolean exists(Long storeId, Long productId, Long warehouseId);

  boolean existsByStoreAndWarehouse(Long storeId, Long warehouseId);

  boolean existsByWarehouseAndProduct(Long warehouseId, Long productId);

  long countDistinctWarehousesByStoreAndProduct(Long storeId, Long productId);

  long countDistinctWarehousesByStore(Long storeId);

  long countDistinctProductsByWarehouse(Long warehouseId);
}
