package com.fulfilment.application.monolith.fulfillment;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class ProductFulfillmentRepository implements PanacheRepository<ProductFulfillment> {

  public List<ProductFulfillment> listByStore(Long storeId) {
    return list("store.id = ?1", storeId);
  }

  public boolean isFulfilling(Long storeId, Long productId, Long warehouseId) {
    return count("store.id = ?1 and product.id = ?2 and warehouse.id = ?3",
            storeId, productId, warehouseId)
        > 0;
  }

  public boolean isWarehouseFulfillingStore(Long storeId, Long warehouseId) {
    return count("store.id = ?1 and warehouse.id = ?2", storeId, warehouseId) > 0;
  }

  public boolean isProductStoredInWarehouse(Long warehouseId, Long productId) {
    return count("warehouse.id = ?1 and product.id = ?2", warehouseId, productId) > 0;
  }

  public long countWarehousesFulfillingProductInStore(Long storeId, Long productId) {
    return countDistinct(
        "select count(distinct f.warehouse.id) from ProductFulfillment f"
            + " where f.store.id = ?1 and f.product.id = ?2",
        storeId,
        productId);
  }

  public long countWarehousesFulfillingStore(Long storeId) {
    return countDistinct(
        "select count(distinct f.warehouse.id) from ProductFulfillment f where f.store.id = ?1",
        storeId);
  }

  public long countProductTypesInWarehouse(Long warehouseId) {
    return countDistinct(
        "select count(distinct f.product.id) from ProductFulfillment f where f.warehouse.id = ?1",
        warehouseId);
  }

  private long countDistinct(String query, Object... parameters) {
    var typedQuery = getEntityManager().createQuery(query, Long.class);

    for (int i = 0; i < parameters.length; i++) {
      typedQuery.setParameter(i + 1, parameters[i]);
    }

    return typedQuery.getSingleResult();
  }
}
