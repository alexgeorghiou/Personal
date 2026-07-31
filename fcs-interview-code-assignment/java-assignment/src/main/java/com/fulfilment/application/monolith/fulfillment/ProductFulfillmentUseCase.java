package com.fulfilment.application.monolith.fulfillment;

import com.fulfilment.application.monolith.exceptions.BusinessRuleException;
import com.fulfilment.application.monolith.exceptions.ResourceNotFoundException;
import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class ProductFulfillmentUseCase {

  public static final int MAX_WAREHOUSES_PER_PRODUCT_IN_STORE = 2;
  public static final int MAX_WAREHOUSES_PER_STORE = 3;
  public static final int MAX_PRODUCT_TYPES_PER_WAREHOUSE = 5;

  @Inject ProductFulfillmentRepository productFulfillmentRepository;

  @Inject ProductRepository productRepository;

  @Inject WarehouseRepository warehouseRepository;

  public List<ProductFulfillment> listByStore(Long storeId) {
    return productFulfillmentRepository.listByStore(storeId);
  }

  @Transactional
  public ProductFulfillment assign(Long storeId, Long productId, String businessUnitCode) {
    Store store = Store.findById(storeId);
    if (store == null) {
      throw new ResourceNotFoundException("Store with id of " + storeId + " does not exist.");
    }

    Product product = productRepository.findById(productId);
    if (product == null) {
      throw new ResourceNotFoundException("Product with id of " + productId + " does not exist.");
    }

    DbWarehouse warehouse =
        warehouseRepository.findActiveEntityByBusinessUnitCode(businessUnitCode);
    if (warehouse == null) {
      throw new ResourceNotFoundException(
          "There is no active warehouse with the business unit code " + businessUnitCode);
    }

    assertNotFulfillingYet(store, product, warehouse);
    assertWarehousesPerProductInStoreNotExceeded(store, product);
    assertWarehousesPerStoreNotExceeded(store, warehouse);
    assertProductTypesPerWarehouseNotExceeded(warehouse, product);

    var fulfillment = new ProductFulfillment(store, product, warehouse);
    productFulfillmentRepository.persist(fulfillment);

    return fulfillment;
  }

  @Transactional
  public void unassign(Long id) {
    var fulfillment = productFulfillmentRepository.findById(id);

    if (fulfillment == null) {
      throw new ResourceNotFoundException("Fulfilment with id of " + id + " does not exist.");
    }

    productFulfillmentRepository.delete(fulfillment);
  }

  private void assertNotFulfillingYet(Store store, Product product, DbWarehouse warehouse) {
    if (productFulfillmentRepository.isFulfilling(store.id, product.id, warehouse.id)) {
      throw new BusinessRuleException(
          "The warehouse "
              + warehouse.businessUnitCode
              + " already fulfils the product "
              + product.name
              + " for the store "
              + store.name);
    }
  }

  private void assertWarehousesPerProductInStoreNotExceeded(Store store, Product product) {
    long warehouses =
        productFulfillmentRepository.countWarehousesFulfillingProductInStore(store.id, product.id);

    if (warehouses >= MAX_WAREHOUSES_PER_PRODUCT_IN_STORE) {
      throw new BusinessRuleException(
          "The product "
              + product.name
              + " is already fulfilled by the maximum of "
              + MAX_WAREHOUSES_PER_PRODUCT_IN_STORE
              + " warehouses on the store "
              + store.name);
    }
  }

  private void assertWarehousesPerStoreNotExceeded(Store store, DbWarehouse warehouse) {
    if (productFulfillmentRepository.isWarehouseFulfillingStore(store.id, warehouse.id)) {
      return;
    }

    if (productFulfillmentRepository.countWarehousesFulfillingStore(store.id)
        >= MAX_WAREHOUSES_PER_STORE) {
      throw new BusinessRuleException(
          "The store "
              + store.name
              + " is already fulfilled by the maximum of "
              + MAX_WAREHOUSES_PER_STORE
              + " warehouses");
    }
  }

  private void assertProductTypesPerWarehouseNotExceeded(DbWarehouse warehouse, Product product) {
    if (productFulfillmentRepository.isProductStoredInWarehouse(warehouse.id, product.id)) {
      return;
    }

    if (productFulfillmentRepository.countProductTypesInWarehouse(warehouse.id)
        >= MAX_PRODUCT_TYPES_PER_WAREHOUSE) {
      throw new BusinessRuleException(
          "The warehouse "
              + warehouse.businessUnitCode
              + " already holds the maximum of "
              + MAX_PRODUCT_TYPES_PER_WAREHOUSE
              + " types of products");
    }
  }
}
