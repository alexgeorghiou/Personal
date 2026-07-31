package com.fulfilment.application.monolith.fulfillment;

import com.fulfilment.application.monolith.exceptions.BusinessRuleException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("fulfillment")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class ProductFulfillmentResource {

  @Inject ProductFulfillmentUseCase productFulfillmentUseCase;

  @GET
  @Path("store/{storeId}")
  public List<ProductFulfillmentResponse> listByStore(@PathParam("storeId") Long storeId) {
    return productFulfillmentUseCase.listByStore(storeId).stream()
        .map(ProductFulfillmentResponse::from)
        .toList();
  }

  @POST
  public Response assign(ProductFulfillmentRequest request) {
    if (request == null
        || request.storeId == null
        || request.productId == null
        || request.warehouseBusinessUnitCode == null) {
      throw new BusinessRuleException(
          "storeId, productId and warehouseBusinessUnitCode are mandatory");
    }

    var fulfillment =
        productFulfillmentUseCase.assign(
            request.storeId, request.productId, request.warehouseBusinessUnitCode);

    return Response.ok(ProductFulfillmentResponse.from(fulfillment)).status(201).build();
  }

  @DELETE
  @Path("{id}")
  public Response unassign(@PathParam("id") Long id) {
    productFulfillmentUseCase.unassign(id);

    return Response.status(204).build();
  }
}
