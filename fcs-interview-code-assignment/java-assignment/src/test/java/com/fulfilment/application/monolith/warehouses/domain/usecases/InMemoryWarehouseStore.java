package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.exceptions.ResourceNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

class InMemoryWarehouseStore implements WarehouseStore {

  private final List<Warehouse> warehouses = new ArrayList<>();

  private long sequence;

  InMemoryWarehouseStore(Warehouse... initialWarehouses) {
    for (Warehouse warehouse : initialWarehouses) {
      create(warehouse);
    }
  }

  @Override
  public List<Warehouse> getAll() {
    return warehouses.stream().filter(warehouse -> warehouse.archivedAt == null).toList();
  }

  @Override
  public void create(Warehouse warehouse) {
    warehouse.id = ++sequence;
    warehouse.createdAt = warehouse.createdAt == null ? LocalDateTime.now() : warehouse.createdAt;

    warehouses.add(warehouse);
  }

  @Override
  public void update(Warehouse warehouse) {
    var stored = find(warehouse);

    if (stored == null) {
      throw new ResourceNotFoundException(
          "There is no warehouse to update for the business unit code "
              + warehouse.businessUnitCode);
    }

    stored.location = warehouse.location;
    stored.capacity = warehouse.capacity;
    stored.stock = warehouse.stock;
    stored.archivedAt = warehouse.archivedAt;
  }

  @Override
  public void remove(Warehouse warehouse) {
    warehouses.remove(find(warehouse));
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    return getAll().stream()
        .filter(warehouse -> warehouse.businessUnitCode.equals(buCode))
        .findFirst()
        .orElse(null);
  }

  @Override
  public Warehouse findActiveById(Long id) {
    return getAll().stream().filter(warehouse -> warehouse.id.equals(id)).findFirst().orElse(null);
  }

  @Override
  public List<Warehouse> findActiveByLocation(String location) {
    return getAll().stream().filter(warehouse -> warehouse.location.equals(location)).toList();
  }

  private Warehouse find(Warehouse warehouse) {
    if (warehouse.id != null) {
      return warehouses.stream()
          .filter(stored -> stored.id.equals(warehouse.id))
          .findFirst()
          .orElse(null);
    }

    return findByBusinessUnitCode(warehouse.businessUnitCode);
  }
}
