package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.exceptions.BusinessRuleException;
import com.fulfilment.application.monolith.exceptions.ResourceNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final ArchiveWarehouseOperation archiveWarehouseOperation;
  private final CreateWarehouseOperation createWarehouseOperation;

  public ReplaceWarehouseUseCase(
      WarehouseStore warehouseStore,
      ArchiveWarehouseOperation archiveWarehouseOperation,
      CreateWarehouseOperation createWarehouseOperation) {
    this.warehouseStore = warehouseStore;
    this.archiveWarehouseOperation = archiveWarehouseOperation;
    this.createWarehouseOperation = createWarehouseOperation;
  }

  @Override
  @Transactional
  public void replace(Warehouse newWarehouse) {
    if (newWarehouse == null
        || newWarehouse.businessUnitCode == null
        || newWarehouse.businessUnitCode.isBlank()) {
      throw new BusinessRuleException("The business unit code is mandatory");
    }

    Warehouse previous = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (previous == null) {
      throw new ResourceNotFoundException(
          "There is no active warehouse with the business unit code "
              + newWarehouse.businessUnitCode);
    }

    int previousStock = previous.stock == null ? 0 : previous.stock;

    if (newWarehouse.location == null || !newWarehouse.location.equalsIgnoreCase(previous.location)) {
      throw new BusinessRuleException(
          "A warehouse can only be replaced by another one on the same location: "
              + previous.location);
    }

    if (newWarehouse.capacity == null || newWarehouse.capacity < previousStock) {
      throw new BusinessRuleException(
          "The new warehouse must be able to accommodate the "
              + previousStock
              + " units held by the warehouse being replaced");
    }

    if (newWarehouse.stock == null || newWarehouse.stock != previousStock) {
      throw new BusinessRuleException(
          "The stock of the new warehouse must match the "
              + previousStock
              + " units of the warehouse being replaced");
    }

    // The business unit code is reused, so the previous unit has to leave the active set before the
    // replacement is created.
    archiveWarehouseOperation.archive(previous);
    createWarehouseOperation.create(newWarehouse);
  }
}
