package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static com.fulfilment.application.monolith.warehouses.domain.usecases.WarehouseFixtures.warehouse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fulfilment.application.monolith.exceptions.BusinessRuleException;
import com.fulfilment.application.monolith.exceptions.ResourceNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

public class ArchiveWarehouseUseCaseTest {

  @Test
  public void testArchiveAnActiveWarehouse() {
    Warehouse warehouse = warehouse("MWH.100", "AMSTERDAM-002", 40, 20);

    var store = new InMemoryWarehouseStore(warehouse);
    var useCase = new ArchiveWarehouseUseCase(store);

    useCase.archive(warehouse);

    assertTrue(store.getAll().isEmpty());
    assertNull(store.findByBusinessUnitCode("MWH.100"));
    assertNotNull(warehouse.archivedAt);
  }

  @Test
  public void testArchiveAWarehouseThatDoesNotExist() {
    var store = new InMemoryWarehouseStore();
    var useCase = new ArchiveWarehouseUseCase(store);

    var exception =
        assertThrows(
            ResourceNotFoundException.class,
            () -> useCase.archive(warehouse("MWH.100", "AMSTERDAM-002", 40, 20)));

    assertEquals(
        "There is no active warehouse with the business unit code MWH.100", exception.getMessage());
  }

  @Test
  public void testArchiveAWarehouseThatIsAlreadyArchived() {
    Warehouse warehouse = warehouse("MWH.100", "AMSTERDAM-002", 40, 20);
    warehouse.archivedAt = LocalDateTime.now();

    var store = new InMemoryWarehouseStore(warehouse);
    var useCase = new ArchiveWarehouseUseCase(store);

    assertThrows(BusinessRuleException.class, () -> useCase.archive(warehouse));
  }
}
