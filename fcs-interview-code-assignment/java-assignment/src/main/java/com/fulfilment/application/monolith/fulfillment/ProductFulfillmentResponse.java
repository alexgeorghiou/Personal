package com.fulfilment.application.monolith.fulfillment;

public class ProductFulfillmentResponse {

  public Long id;

  public Long storeId;

  public String storeName;

  public Long productId;

  public String productName;

  public String warehouseBusinessUnitCode;

  public static ProductFulfillmentResponse from(ProductFulfillment fulfillment) {
    var response = new ProductFulfillmentResponse();
    response.id = fulfillment.id;
    response.storeId = fulfillment.store.id;
    response.storeName = fulfillment.store.name;
    response.productId = fulfillment.product.id;
    response.productName = fulfillment.product.name;
    response.warehouseBusinessUnitCode = fulfillment.warehouse.businessUnitCode;

    return response;
  }
}
