package com.fulfilment.application.monolith.fulfillment;

import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** States that a Warehouse acts as a fulfilment unit of a given Product for a given Store. */
@Entity
@Table(
    name = "product_fulfillment",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_product_fulfillment",
            columnNames = {"store_id", "product_id", "warehouse_id"}))
public class ProductFulfillment extends PanacheEntity {

  @ManyToOne(optional = false)
  @JoinColumn(name = "store_id")
  public Store store;

  @ManyToOne(optional = false)
  @JoinColumn(name = "product_id")
  public Product product;

  @ManyToOne(optional = false)
  @JoinColumn(name = "warehouse_id")
  public DbWarehouse warehouse;

  public ProductFulfillment() {}

  public ProductFulfillment(Store store, Product product, DbWarehouse warehouse) {
    this.store = store;
    this.product = product;
    this.warehouse = warehouse;
  }
}
