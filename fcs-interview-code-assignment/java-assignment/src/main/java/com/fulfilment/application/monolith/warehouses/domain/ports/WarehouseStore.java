package com.fulfilment.application.monolith.warehouses.domain.ports;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import java.util.List;

/**
 * Archived warehouses are kept for historical reasons only, so every lookup exposed here is
 * restricted to the warehouses that are currently active.
 */
public interface WarehouseStore {

  List<Warehouse> getAll();

  void create(Warehouse warehouse);

  void update(Warehouse warehouse);

  void remove(Warehouse warehouse);

  Warehouse findByBusinessUnitCode(String buCode);

  Warehouse findActiveById(Long id);

  List<Warehouse> findActiveByLocation(String location);
}
