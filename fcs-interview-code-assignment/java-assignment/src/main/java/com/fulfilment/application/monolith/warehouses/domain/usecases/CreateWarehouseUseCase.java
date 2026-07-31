package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.exceptions.BusinessRuleException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public CreateWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  @Transactional
  public void create(Warehouse warehouse) {
    validatePayload(warehouse);

    if (warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode) != null) {
      throw new BusinessRuleException(
          "There is already an active warehouse with the business unit code "
              + warehouse.businessUnitCode);
    }

    Location location = locationResolver.resolveByIdentifier(warehouse.location);
    if (location == null) {
      throw new BusinessRuleException("The location " + warehouse.location + " does not exist");
    }

    List<Warehouse> current = warehouseStore.findActiveByLocation(location.identification);

    if (current.size() >= location.maxNumberOfWarehouses) {
      throw new BusinessRuleException(
          "The location "
              + location.identification
              + " already holds the maximum of "
              + location.maxNumberOfWarehouses
              + " warehouses");
    }

    int occupiedCapacity = current.stream().mapToInt(w -> w.capacity == null ? 0 : w.capacity).sum();
    int freeCapacity = location.maxCapacity - occupiedCapacity;

    if (warehouse.capacity > freeCapacity) {
      throw new BusinessRuleException(
          "The location "
              + location.identification
              + " has "
              + freeCapacity
              + " capacity units left and the warehouse asks for "
              + warehouse.capacity);
    }

    warehouse.location = location.identification;
    warehouse.createdAt = LocalDateTime.now();
    warehouse.archivedAt = null;

    warehouseStore.create(warehouse);
  }

  private void validatePayload(Warehouse warehouse) {
    if (warehouse == null) {
      throw new BusinessRuleException("No warehouse was provided");
    }
    if (warehouse.businessUnitCode == null || warehouse.businessUnitCode.isBlank()) {
      throw new BusinessRuleException("The business unit code is mandatory");
    }
    if (warehouse.location == null || warehouse.location.isBlank()) {
      throw new BusinessRuleException("The location is mandatory");
    }
    if (warehouse.capacity == null || warehouse.capacity <= 0) {
      throw new BusinessRuleException("The capacity must be greater than zero");
    }
    if (warehouse.stock == null || warehouse.stock < 0) {
      throw new BusinessRuleException("The stock cannot be negative");
    }
    if (warehouse.stock > warehouse.capacity) {
      throw new BusinessRuleException(
          "A stock of " + warehouse.stock + " does not fit in a capacity of " + warehouse.capacity);
    }
  }
}
