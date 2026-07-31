package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.exceptions.BusinessRuleException;
import com.fulfilment.application.monolith.exceptions.ResourceNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;

@ApplicationScoped
public class ArchiveWarehouseUseCase implements ArchiveWarehouseOperation {

  private final WarehouseStore warehouseStore;

  public ArchiveWarehouseUseCase(WarehouseStore warehouseStore) {
    this.warehouseStore = warehouseStore;
  }

  @Override
  @Transactional
  public void archive(Warehouse warehouse) {
    if (warehouse == null || warehouse.businessUnitCode == null) {
      throw new ResourceNotFoundException("The warehouse to archive was not found");
    }

    if (warehouse.archivedAt != null) {
      throw new BusinessRuleException(
          "The warehouse " + warehouse.businessUnitCode + " is already archived");
    }

    Warehouse active = warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode);
    if (active == null) {
      throw new ResourceNotFoundException(
          "There is no active warehouse with the business unit code "
              + warehouse.businessUnitCode);
    }

    active.archivedAt = LocalDateTime.now();

    warehouseStore.update(active);
  }
}
