package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static com.fulfilment.application.monolith.warehouses.domain.usecases.WarehouseFixtures.warehouse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.exceptions.BusinessRuleException;
import com.fulfilment.application.monolith.exceptions.ResourceNotFoundException;
import com.fulfilment.application.monolith.location.LocationGateway;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import org.junit.jupiter.api.Test;

public class ReplaceWarehouseUseCaseTest {

  private InMemoryWarehouseStore store;

  private ReplaceWarehouseUseCase useCaseWith(Warehouse... initialWarehouses) {
    store = new InMemoryWarehouseStore(initialWarehouses);

    var locationResolver = new LocationGateway();

    return new ReplaceWarehouseUseCase(
        store,
        new ArchiveWarehouseUseCase(store),
        new CreateWarehouseUseCase(store, locationResolver));
  }

  @Test
  public void testReplaceAnActiveWarehouse() {
    Warehouse previous = warehouse("MWH.100", "TILBURG-001", 30, 10);
    var useCase = useCaseWith(previous);

    useCase.replace(warehouse("MWH.100", "TILBURG-001", 35, 10));

    assertNotNull(previous.archivedAt);
    assertEquals(1, store.getAll().size());

    var active = store.findByBusinessUnitCode("MWH.100");
    assertEquals(35, active.capacity.intValue());
    assertEquals(10, active.stock.intValue());
    assertNotNull(active.createdAt);
  }

  @Test
  public void testReplaceAWarehouseThatDoesNotExist() {
    var useCase = useCaseWith();

    assertThrows(
        ResourceNotFoundException.class,
        () -> useCase.replace(warehouse("MWH.100", "TILBURG-001", 35, 10)));
  }

  @Test
  public void testReplaceAWarehouseByOneOnAnotherLocation() {
    var useCase = useCaseWith(warehouse("MWH.100", "TILBURG-001", 30, 10));

    var exception =
        assertThrows(
            BusinessRuleException.class,
            () -> useCase.replace(warehouse("MWH.100", "EINDHOVEN-001", 35, 10)));

    assertEquals(
        "A warehouse can only be replaced by another one on the same location: TILBURG-001",
        exception.getMessage());
  }

  @Test
  public void testReplaceAWarehouseByOneThatCannotAccommodateItsStock() {
    var useCase = useCaseWith(warehouse("MWH.100", "TILBURG-001", 30, 10));

    var exception =
        assertThrows(
            BusinessRuleException.class,
            () -> useCase.replace(warehouse("MWH.100", "TILBURG-001", 8, 10)));

    assertEquals(
        "The new warehouse must be able to accommodate the 10 units held by the warehouse being"
            + " replaced",
        exception.getMessage());
  }

  @Test
  public void testReplaceAWarehouseByOneWithADifferentStock() {
    var useCase = useCaseWith(warehouse("MWH.100", "TILBURG-001", 30, 10));

    var exception =
        assertThrows(
            BusinessRuleException.class,
            () -> useCase.replace(warehouse("MWH.100", "TILBURG-001", 35, 12)));

    assertEquals(
        "The stock of the new warehouse must match the 10 units of the warehouse being replaced",
        exception.getMessage());
  }

  @Test
  public void testTheWarehouseBeingReplacedDoesNotCountForTheLocationLimits() {
    // TILBURG-001 only accepts one warehouse, so the replacement can only fit once the previous
    // unit has been archived.
    var useCase = useCaseWith(warehouse("MWH.100", "TILBURG-001", 30, 10));

    useCase.replace(warehouse("MWH.100", "TILBURG-001", 40, 10));

    assertEquals(1, store.getAll().size());
  }
}
