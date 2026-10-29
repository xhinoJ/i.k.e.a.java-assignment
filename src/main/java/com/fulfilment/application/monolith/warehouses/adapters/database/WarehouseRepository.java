package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  @Override
  public List<Warehouse> getAll() {
    return find("archivedAt is null").list().stream().map(DbWarehouse::toWarehouse).toList();
  }

  @Override
  public void create(Warehouse warehouse) {
    DbWarehouse entity = new DbWarehouse();
    entity.businessUnitCode = warehouse.businessUnitCode;
    entity.location = warehouse.location;
    entity.capacity = warehouse.capacity;
    entity.stock = warehouse.stock;
    entity.createdAt = warehouse.createdAt != null ? warehouse.createdAt : LocalDateTime.now();
    entity.archivedAt = warehouse.archivedAt;

    persist(entity);

    warehouse.id = entity.id;
    warehouse.createdAt = entity.createdAt;
  }

  @Override
  public void update(Warehouse warehouse) {
    DbWarehouse entity = findActive(warehouse);
    if (entity == null) {
      throw new IllegalStateException(
          "Active warehouse not found for business unit code " + warehouse.businessUnitCode);
    }

    entity.location = warehouse.location;
    entity.capacity = warehouse.capacity;
    entity.stock = warehouse.stock;
    entity.archivedAt = warehouse.archivedAt;

    warehouse.id = entity.id;
  }

  @Override
  public void remove(Warehouse warehouse) {
    DbWarehouse entity = findActive(warehouse);
    if (entity == null) {
      throw new IllegalStateException(
          "Active warehouse not found for business unit code " + warehouse.businessUnitCode);
    }

    entity.archivedAt = LocalDateTime.now();
    warehouse.archivedAt = entity.archivedAt;
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    DbWarehouse entity =
        find("businessUnitCode = ?1 and archivedAt is null", buCode).firstResult();
    return entity == null ? null : entity.toWarehouse();
  }

  @Override
  public Warehouse findWarehouseById(Long id) {
    DbWarehouse entity = find("id = ?1 and archivedAt is null", id).firstResult();
    return entity == null ? null : entity.toWarehouse();
  }

  @Override
  public long countByLocation(String location) {
    return count("location = ?1 and archivedAt is null", location);
  }

  @Override
  public long sumCapacityByLocation(String location) {
    Long total =
        getEntityManager()
            .createQuery(
                "select coalesce(sum(w.capacity), 0) from DbWarehouse w "
                    + "where w.location = :location and w.archivedAt is null",
                Long.class)
            .setParameter("location", location)
            .getSingleResult();
    return total == null ? 0L : total;
  }

  /**
   * Resolves the active database entity represented by the given domain object. The database id is
   * preferred when available; otherwise the business unit code is used.
   */
  private DbWarehouse findActive(Warehouse warehouse) {
    if (warehouse.id != null) {
      DbWarehouse byId = findById(warehouse.id);
      if (byId != null && byId.archivedAt == null) {
        return byId;
      }
    }

    if (warehouse.businessUnitCode != null) {
      return find("businessUnitCode = ?1 and archivedAt is null", warehouse.businessUnitCode)
          .firstResult();
    }

    return null;
  }
}
