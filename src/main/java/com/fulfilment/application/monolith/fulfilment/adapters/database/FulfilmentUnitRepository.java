package com.fulfilment.application.monolith.fulfilment.adapters.database;

import com.fulfilment.application.monolith.fulfilment.domain.models.FulfilmentUnit;
import com.fulfilment.application.monolith.fulfilment.domain.ports.FulfilmentUnitStore;
import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class FulfilmentUnitRepository
    implements FulfilmentUnitStore, PanacheRepository<DbFulfilmentUnit> {

  @Override
  public List<FulfilmentUnit> getAll() {
    return find("warehouse.archivedAt is null").list().stream()
        .map(DbFulfilmentUnit::toFulfilmentUnit)
        .toList();
  }

  @Override
  public FulfilmentUnit findFulfilmentUnitById(Long id) {
    DbFulfilmentUnit entity =
        find("id = ?1 and warehouse.archivedAt is null", id).firstResult();
    return entity == null ? null : entity.toFulfilmentUnit();
  }

  @Override
  public FulfilmentUnit create(FulfilmentUnit unit) {
    DbFulfilmentUnit entity = new DbFulfilmentUnit();
    entity.store = getEntityManager().find(Store.class, unit.storeId);
    entity.product = getEntityManager().find(Product.class, unit.productId);
    entity.warehouse = getEntityManager().find(DbWarehouse.class, unit.warehouseId);

    persist(entity);

    unit.id = entity.id;
    return unit;
  }

  @Override
  public void remove(FulfilmentUnit unit) {
    DbFulfilmentUnit entity = findById(unit.id);
    if (entity != null) {
      delete(entity);
    }
  }

  @Override
  public boolean exists(Long storeId, Long productId, Long warehouseId) {
    return count(
            "store.id = ?1 and product.id = ?2 and warehouse.id = ?3 "
                + "and warehouse.archivedAt is null",
            storeId,
            productId,
            warehouseId)
        > 0;
  }

  @Override
  public boolean existsByStoreAndWarehouse(Long storeId, Long warehouseId) {
    return count(
            "store.id = ?1 and warehouse.id = ?2 and warehouse.archivedAt is null",
            storeId,
            warehouseId)
        > 0;
  }

  @Override
  public boolean existsByWarehouseAndProduct(Long warehouseId, Long productId) {
    return count(
            "warehouse.id = ?1 and product.id = ?2 and warehouse.archivedAt is null",
            warehouseId,
            productId)
        > 0;
  }

  @Override
  public long countDistinctWarehousesByStoreAndProduct(Long storeId, Long productId) {
    return getEntityManager()
        .createQuery(
            "select count(distinct f.warehouse.id) from DbFulfilmentUnit f "
                + "where f.store.id = :storeId and f.product.id = :productId "
                + "and f.warehouse.archivedAt is null",
            Long.class)
        .setParameter("storeId", storeId)
        .setParameter("productId", productId)
        .getSingleResult();
  }

  @Override
  public long countDistinctWarehousesByStore(Long storeId) {
    return getEntityManager()
        .createQuery(
            "select count(distinct f.warehouse.id) from DbFulfilmentUnit f "
                + "where f.store.id = :storeId and f.warehouse.archivedAt is null",
            Long.class)
        .setParameter("storeId", storeId)
        .getSingleResult();
  }

  @Override
  public long countDistinctProductsByWarehouse(Long warehouseId) {
    return getEntityManager()
        .createQuery(
            "select count(distinct f.product.id) from DbFulfilmentUnit f "
                + "where f.warehouse.id = :warehouseId and f.warehouse.archivedAt is null",
            Long.class)
        .setParameter("warehouseId", warehouseId)
        .getSingleResult();
  }
}
