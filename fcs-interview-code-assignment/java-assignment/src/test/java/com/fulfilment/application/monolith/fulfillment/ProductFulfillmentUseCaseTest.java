package com.fulfilment.application.monolith.fulfillment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.exceptions.BusinessRuleException;
import com.fulfilment.application.monolith.exceptions.ResourceNotFoundException;
import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ProductFulfillmentUseCaseTest {

  @Inject ProductFulfillmentUseCase productFulfillmentUseCase;

  @Inject ProductRepository productRepository;

  @Inject WarehouseRepository warehouseRepository;

  @Test
  @TestTransaction
  public void testAssignAWarehouseAsFulfilmentUnitOfAProduct() {
    Store store = store("FLT-STORE-1");
    Product product = product("FLT-PRODUCT-1");
    DbWarehouse warehouse = warehouse("FLT.001");

    var fulfillment = productFulfillmentUseCase.assign(store.id, product.id, "FLT.001");

    assertNotNull(fulfillment.id);
    assertEquals(warehouse.id, fulfillment.warehouse.id);
    assertEquals(1, productFulfillmentUseCase.listByStore(store.id).size());
  }

  @Test
  @TestTransaction
  public void testAssignTheSameWarehouseTwiceForTheSameProductAndStore() {
    Store store = store("FLT-STORE-2");
    Product product = product("FLT-PRODUCT-2");
    warehouse("FLT.002");

    productFulfillmentUseCase.assign(store.id, product.id, "FLT.002");

    assertThrows(
        BusinessRuleException.class,
        () -> productFulfillmentUseCase.assign(store.id, product.id, "FLT.002"));
  }

  @Test
  @TestTransaction
  public void testAProductCannotBeFulfilledByMoreThanTwoWarehousesPerStore() {
    Store store = store("FLT-STORE-3");
    Product product = product("FLT-PRODUCT-3");
    warehouse("FLT.010");
    warehouse("FLT.011");
    warehouse("FLT.012");

    productFulfillmentUseCase.assign(store.id, product.id, "FLT.010");
    productFulfillmentUseCase.assign(store.id, product.id, "FLT.011");

    var exception =
        assertThrows(
            BusinessRuleException.class,
            () -> productFulfillmentUseCase.assign(store.id, product.id, "FLT.012"));

    assertEquals(
        "The product FLT-PRODUCT-3 is already fulfilled by the maximum of 2 warehouses on the"
            + " store FLT-STORE-3",
        exception.getMessage());
  }

  @Test
  @TestTransaction
  public void testAStoreCannotBeFulfilledByMoreThanThreeWarehouses() {
    Store store = store("FLT-STORE-4");
    Long first = product("FLT-PRODUCT-40").id;
    Long second = product("FLT-PRODUCT-41").id;
    Long third = product("FLT-PRODUCT-42").id;
    Long fourth = product("FLT-PRODUCT-43").id;
    warehouse("FLT.020");
    warehouse("FLT.021");
    warehouse("FLT.022");
    warehouse("FLT.023");

    productFulfillmentUseCase.assign(store.id, first, "FLT.020");
    productFulfillmentUseCase.assign(store.id, second, "FLT.021");
    productFulfillmentUseCase.assign(store.id, third, "FLT.022");

    var exception =
        assertThrows(
            BusinessRuleException.class,
            () -> productFulfillmentUseCase.assign(store.id, fourth, "FLT.023"));

    assertEquals(
        "The store FLT-STORE-4 is already fulfilled by the maximum of 3 warehouses",
        exception.getMessage());
  }

  @Test
  @TestTransaction
  public void testAWarehouseCannotHoldMoreThanFiveTypesOfProducts() {
    Store store = store("FLT-STORE-5");
    warehouse("FLT.030");

    for (int i = 1; i <= 5; i++) {
      productFulfillmentUseCase.assign(store.id, product("FLT-PRODUCT-5" + i).id, "FLT.030");
    }

    Long sixth = product("FLT-PRODUCT-56").id;

    var exception =
        assertThrows(
            BusinessRuleException.class,
            () -> productFulfillmentUseCase.assign(store.id, sixth, "FLT.030"));

    assertEquals(
        "The warehouse FLT.030 already holds the maximum of 5 types of products",
        exception.getMessage());
  }

  @Test
  @TestTransaction
  public void testAssignAnUnknownStoreProductOrWarehouse() {
    Store store = store("FLT-STORE-6");
    Product product = product("FLT-PRODUCT-6");
    warehouse("FLT.040");

    assertThrows(
        ResourceNotFoundException.class,
        () -> productFulfillmentUseCase.assign(999999L, product.id, "FLT.040"));
    assertThrows(
        ResourceNotFoundException.class,
        () -> productFulfillmentUseCase.assign(store.id, 999999L, "FLT.040"));
    assertThrows(
        ResourceNotFoundException.class,
        () -> productFulfillmentUseCase.assign(store.id, product.id, "FLT.999"));
  }

  private Store store(String name) {
    var store = new Store(name);
    store.quantityProductsInStock = 0;
    store.persist();

    return store;
  }

  private Product product(String name) {
    var product = new Product(name);
    productRepository.persist(product);

    return product;
  }

  private DbWarehouse warehouse(String businessUnitCode) {
    var warehouse = new DbWarehouse();
    warehouse.businessUnitCode = businessUnitCode;
    warehouse.location = "AMSTERDAM-001";
    warehouse.capacity = 10;
    warehouse.stock = 0;
    warehouse.createdAt = LocalDateTime.now();

    warehouseRepository.persist(warehouse);

    return warehouse;
  }
}
