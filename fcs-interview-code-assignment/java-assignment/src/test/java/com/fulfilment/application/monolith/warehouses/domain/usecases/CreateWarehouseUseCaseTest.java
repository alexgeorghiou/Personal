package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static com.fulfilment.application.monolith.warehouses.domain.usecases.WarehouseFixtures.warehouse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.exceptions.BusinessRuleException;
import com.fulfilment.application.monolith.location.LocationGateway;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

public class CreateWarehouseUseCaseTest {

  @Test
  public void testCreateWarehouseOnAValidLocation() {
    var store = new InMemoryWarehouseStore();
    var useCase = new CreateWarehouseUseCase(store, new LocationGateway());

    var warehouse = warehouse("MWH.100", "AMSTERDAM-002", 40, 20);

    useCase.create(warehouse);

    assertEquals(1, store.getAll().size());
    assertNotNull(warehouse.createdAt);

    var created = store.findByBusinessUnitCode("MWH.100");
    assertEquals("AMSTERDAM-002", created.location);
    assertEquals(40, created.capacity.intValue());
    assertEquals(20, created.stock.intValue());
  }

  @Test
  public void testCreateWarehouseWithAnAlreadyUsedBusinessUnitCode() {
    var store = new InMemoryWarehouseStore(warehouse("MWH.100", "AMSTERDAM-002", 40, 20));
    var useCase = new CreateWarehouseUseCase(store, new LocationGateway());

    var exception =
        assertThrows(
            BusinessRuleException.class,
            () -> useCase.create(warehouse("MWH.100", "AMSTERDAM-002", 10, 5)));

    assertEquals(
        "There is already an active warehouse with the business unit code MWH.100",
        exception.getMessage());
  }

  @Test
  public void testCreateWarehouseOnAnUnknownLocation() {
    var store = new InMemoryWarehouseStore();
    var useCase = new CreateWarehouseUseCase(store, new LocationGateway());

    assertThrows(
        BusinessRuleException.class,
        () -> useCase.create(warehouse("MWH.100", "ROTTERDAM-001", 10, 5)));

    assertEquals(0, store.getAll().size());
  }

  @Test
  public void testCreateWarehouseOnALocationThatIsFull() {
    var store = new InMemoryWarehouseStore(warehouse("MWH.100", "TILBURG-001", 10, 5));
    var useCase = new CreateWarehouseUseCase(store, new LocationGateway());

    var exception =
        assertThrows(
            BusinessRuleException.class,
            () -> useCase.create(warehouse("MWH.101", "TILBURG-001", 10, 5)));

    assertEquals(
        "The location TILBURG-001 already holds the maximum of 1 warehouses",
        exception.getMessage());
  }

  @Test
  public void testCreateWarehouseBeyondTheCapacityOfTheLocation() {
    var store = new InMemoryWarehouseStore(warehouse("MWH.100", "ZWOLLE-002", 30, 5));
    var useCase = new CreateWarehouseUseCase(store, new LocationGateway());

    var exception =
        assertThrows(
            BusinessRuleException.class,
            () -> useCase.create(warehouse("MWH.101", "ZWOLLE-002", 25, 5)));

    assertEquals(
        "The total capacity of the warehouses on location ZWOLLE-002 would become 55, which"
            + " exceeds its maximum capacity of 50",
        exception.getMessage());
  }

  @Test
  public void testTheCapacityOfEveryWarehouseOnTheLocationCounts() {
    // AMSTERDAM-001 accepts 5 warehouses and 100 capacity units in total
    var store =
        new InMemoryWarehouseStore(
            warehouse("MWH.100", "AMSTERDAM-001", 40, 10),
            warehouse("MWH.101", "AMSTERDAM-001", 30, 10),
            warehouse("MWH.102", "AMSTERDAM-001", 20, 10));
    var useCase = new CreateWarehouseUseCase(store, new LocationGateway());

    var exception =
        assertThrows(
            BusinessRuleException.class,
            () -> useCase.create(warehouse("MWH.103", "AMSTERDAM-001", 15, 5)));

    assertEquals(
        "The total capacity of the warehouses on location AMSTERDAM-001 would become 105, which"
            + " exceeds its maximum capacity of 100",
        exception.getMessage());

    // the location still has room for exactly 10 units
    useCase.create(warehouse("MWH.103", "AMSTERDAM-001", 10, 5));

    assertEquals(4, store.getAll().size());
  }

  @Test
  public void testTheCapacityOfArchivedWarehousesIsNotCounted() {
    Warehouse archived = warehouse("MWH.100", "TILBURG-001", 40, 0);
    archived.archivedAt = LocalDateTime.now();

    var store = new InMemoryWarehouseStore(archived);
    var useCase = new CreateWarehouseUseCase(store, new LocationGateway());

    useCase.create(warehouse("MWH.101", "TILBURG-001", 40, 5));

    assertEquals(1, store.getAll().size());
  }

  @Test
  public void testCreateWarehouseHoldingMoreStockThanItsCapacity() {
    var store = new InMemoryWarehouseStore();
    var useCase = new CreateWarehouseUseCase(store, new LocationGateway());

    var exception =
        assertThrows(
            BusinessRuleException.class,
            () -> useCase.create(warehouse("MWH.100", "AMSTERDAM-002", 10, 11)));

    assertEquals("A stock of 11 does not fit in a capacity of 10", exception.getMessage());
  }

  @Test
  public void testCreateWarehouseWithoutTheMandatoryFields() {
    var store = new InMemoryWarehouseStore();
    var useCase = new CreateWarehouseUseCase(store, new LocationGateway());

    assertThrows(
        BusinessRuleException.class, () -> useCase.create(warehouse(null, "ZWOLLE-002", 10, 5)));
    assertThrows(
        BusinessRuleException.class, () -> useCase.create(warehouse("MWH.100", null, 10, 5)));
    assertThrows(
        BusinessRuleException.class,
        () -> useCase.create(warehouse("MWH.100", "ZWOLLE-002", 0, 0)));
  }
}
