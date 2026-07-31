package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.exceptions.BusinessRuleException;
import com.fulfilment.application.monolith.exceptions.ResourceNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.warehouse.api.WarehouseResource;
import com.warehouse.api.beans.Warehouse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.jboss.resteasy.reactive.ResponseStatus;

@RequestScoped
public class WarehouseResourceImpl implements WarehouseResource {

  @Inject WarehouseStore warehouseStore;

  @Inject CreateWarehouseOperation createWarehouseOperation;

  @Inject ReplaceWarehouseOperation replaceWarehouseOperation;

  @Inject ArchiveWarehouseOperation archiveWarehouseOperation;

  @Override
  public List<Warehouse> listAllWarehousesUnits() {
    return warehouseStore.getAll().stream().map(this::toWarehouseResponse).toList();
  }

  @Override
  @ResponseStatus(201)
  public Warehouse createANewWarehouseUnit(@NotNull Warehouse data) {
    var warehouse = toWarehouseModel(data);

    createWarehouseOperation.create(warehouse);

    return toWarehouseResponse(warehouse);
  }

  @Override
  public Warehouse getAWarehouseUnitByID(String id) {
    return toWarehouseResponse(getActiveWarehouseOrFail(id));
  }

  @Override
  public void archiveAWarehouseUnitByID(String id) {
    archiveWarehouseOperation.archive(getActiveWarehouseOrFail(id));
  }

  @Override
  public Warehouse replaceTheCurrentActiveWarehouse(
      String businessUnitCode, @NotNull Warehouse data) {
    var warehouse = toWarehouseModel(data);

    if (warehouse.businessUnitCode != null
        && !warehouse.businessUnitCode.equals(businessUnitCode)) {
      throw new BusinessRuleException(
          "The business unit code of the replacement must be "
              + businessUnitCode
              + ", the one of the warehouse being replaced");
    }

    warehouse.businessUnitCode = businessUnitCode;

    replaceWarehouseOperation.replace(warehouse);

    return toWarehouseResponse(warehouse);
  }

  private com.fulfilment.application.monolith.warehouses.domain.models.Warehouse
      getActiveWarehouseOrFail(String id) {
    var warehouse = warehouseStore.findActiveById(toWarehouseId(id));

    if (warehouse == null) {
      throw new ResourceNotFoundException("Warehouse unit " + id + " was not found");
    }

    return warehouse;
  }

  private Long toWarehouseId(String id) {
    try {
      return Long.valueOf(id);
    } catch (NumberFormatException e) {
      throw new ResourceNotFoundException("Warehouse unit " + id + " was not found");
    }
  }

  private com.fulfilment.application.monolith.warehouses.domain.models.Warehouse toWarehouseModel(
      Warehouse data) {
    if (data == null) {
      throw new BusinessRuleException("No warehouse was provided");
    }

    var warehouse = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    warehouse.businessUnitCode = data.getBusinessUnitCode();
    warehouse.location = data.getLocation();
    warehouse.capacity = data.getCapacity();
    warehouse.stock = data.getStock();

    return warehouse;
  }

  private Warehouse toWarehouseResponse(
      com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse) {
    var response = new Warehouse();
    response.setId(warehouse.id == null ? null : String.valueOf(warehouse.id));
    response.setBusinessUnitCode(warehouse.businessUnitCode);
    response.setLocation(warehouse.location);
    response.setCapacity(warehouse.capacity);
    response.setStock(warehouse.stock);

    return response;
  }
}
