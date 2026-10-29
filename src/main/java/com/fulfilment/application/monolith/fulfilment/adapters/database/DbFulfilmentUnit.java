package com.fulfilment.application.monolith.fulfilment.adapters.database;

import com.fulfilment.application.monolith.fulfilment.domain.models.FulfilmentUnit;
import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(
    name = "fulfilment_unit",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_fulfilment_unit_store_product_warehouse",
            columnNames = {"store_id", "product_id", "warehouse_id"}))
public class DbFulfilmentUnit {

  @Id @GeneratedValue public Long id;

  @ManyToOne
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "store_id", nullable = false)
  public Store store;

  @ManyToOne
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "product_id", nullable = false)
  public Product product;

  @ManyToOne
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "warehouse_id", nullable = false)
  public DbWarehouse warehouse;

  public DbFulfilmentUnit() {}

  public FulfilmentUnit toFulfilmentUnit() {
    var unit = new FulfilmentUnit();
    unit.id = this.id;
    unit.storeId = this.store != null ? this.store.id : null;
    unit.productId = this.product != null ? this.product.id : null;
    unit.warehouseId = this.warehouse != null ? this.warehouse.id : null;
    return unit;
  }
}
