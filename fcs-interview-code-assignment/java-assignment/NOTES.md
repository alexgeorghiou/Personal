# Implementation notes

A few decisions and assumptions that are not obvious from the diff.

## Warehouse

- `WarehouseStore.getAll()` returns only the active units. Archived warehouses are kept
  as history of a business unit code, so listing them next to the live ones would be
  misleading, and the archiving integration test expects the archived unit to disappear
  from `GET /warehouse`.
- The `{id}` in `GET /warehouse/{id}` and `DELETE /warehouse/{id}` is the entity id, not
  the business unit code, since the schema carries both. An id that is not a number is
  answered with a 404 rather than a 400: the resource simply does not exist.
- A replacement has to stay on the same location as the unit it replaces. The briefing
  describes the operation as building a new warehouse in the same area, and without that
  rule the business unit code would silently move between cities.
- `ReplaceWarehouseUseCase` archives first and then delegates to `CreateWarehouseUseCase`.
  Reusing the creation rules means the replacement is validated exactly like any other
  new unit, and the unit on its way out no longer counts against the location limits,
  which matters for a location that only accepts one warehouse.
- The whole replacement runs in a single transaction, so a rejected replacement does not
  leave the previous unit archived.
- Domain errors are `BusinessRuleException` (400) and `ResourceNotFoundException` (404),
  mapped to responses in the REST layer so the domain stays free of JAX-RS.

Note that the seed data in `import.sql` does not respect the location limits: MWH.001
has a capacity of 100 on ZWOLLE-001, which allows 40. I left it untouched, the rules are
only applied to new units.

## Store

The calls to the legacy system moved out of `StoreResource` into
`LegacyStoreSynchronizer`, which observes a `StoreChangedEvent` during
`TransactionPhase.AFTER_SUCCESS`. The resource fires the event, CDI holds it until the
transaction commits, and a rollback drops it. The event carries a snapshot of the values
instead of the entity, because it is consumed once the entity is no longer managed.

Two things were fixed along the way: the update handlers were sending the request object,
which has no id, instead of the persisted one, and `PATCH` was refusing a body without a
name and reading the wrong side when deciding which fields to apply.

A failure downstream is logged and swallowed. Our data is already committed at that
point, so failing the request would tell the caller something untrue. A real system would
need an outbox and a retry here, which felt out of scope.

## Fulfilment (bonus)

`ProductFulfillment` joins a store, a product and a warehouse, with the three limits
enforced in `ProductFulfillmentUseCase`. Adding a warehouse that already serves the
store, or a product that the warehouse already holds, does not count against the store
and warehouse limits, since neither adds a new distinct entry.

This part deliberately follows the Product style, a Panache repository with DTOs, rather
than the hexagonal Warehouse style. It is a small feature with no domain model of its
own yet.

## Tests

- The use case tests run without a container, against an in memory `WarehouseStore` and
  the real `LocationGateway`.
- `StoreEndpointTest` proves the ordering: a store that violates the unique name
  constraint fails on commit, and the legacy synchronizer never sees it.
- The fulfilment tests use `@TestTransaction` and create their own data, so they do not
  depend on the seed data or on each other.
- The endpoint tests pick locations that the seed data leaves free, so they can run in
  any order.
