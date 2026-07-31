package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.exceptions.ResourceNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  private static final String ACTIVE = "archivedAt is null";

  @Override
  public List<Warehouse> getAll() {
    return this.find(ACTIVE, Sort.by("businessUnitCode")).list().stream()
        .map(DbWarehouse::toWarehouse)
        .toList();
  }

  @Override
  @Transactional
  public void create(Warehouse warehouse) {
    var entity = new DbWarehouse();

    warehouse.createdAt = warehouse.createdAt == null ? LocalDateTime.now() : warehouse.createdAt;
    entity.applyChangesFrom(warehouse);

    this.persist(entity);

    warehouse.id = entity.id;
  }

  @Override
  @Transactional
  public void update(Warehouse warehouse) {
    var entity = findEntity(warehouse);

    if (entity == null) {
      throw new ResourceNotFoundException(
          "There is no warehouse to update for the business unit code "
              + warehouse.businessUnitCode);
    }

    entity.applyChangesFrom(warehouse);
  }

  @Override
  @Transactional
  public void remove(Warehouse warehouse) {
    var entity = findEntity(warehouse);

    if (entity != null) {
      this.delete(entity);
    }
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    var entity = findActiveEntityByBusinessUnitCode(buCode);

    return entity == null ? null : entity.toWarehouse();
  }

  @Override
  public Warehouse findActiveById(Long id) {
    if (id == null) {
      return null;
    }

    var entity = this.find("id = ?1 and " + ACTIVE, id).firstResult();

    return entity == null ? null : entity.toWarehouse();
  }

  @Override
  public List<Warehouse> findActiveByLocation(String location) {
    if (location == null) {
      return List.of();
    }

    return this.find("location = ?1 and " + ACTIVE, location).list().stream()
        .map(DbWarehouse::toWarehouse)
        .toList();
  }

  public DbWarehouse findActiveEntityByBusinessUnitCode(String buCode) {
    if (buCode == null) {
      return null;
    }

    return this.find("businessUnitCode = ?1 and " + ACTIVE, buCode).firstResult();
  }

  private DbWarehouse findEntity(Warehouse warehouse) {
    if (warehouse.id != null) {
      return this.findById(warehouse.id);
    }

    return findActiveEntityByBusinessUnitCode(warehouse.businessUnitCode);
  }
}
