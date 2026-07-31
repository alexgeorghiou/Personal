package com.fulfilment.application.monolith.stores;

/**
 * Snapshot of a Store change. It carries plain values instead of the entity itself because it is
 * consumed after the transaction is committed, when the entity is no longer managed.
 */
public record StoreChangedEvent(Change change, Long id, String name, int quantityProductsInStock) {

  public enum Change {
    CREATED,
    UPDATED
  }

  public static StoreChangedEvent created(Store store) {
    return new StoreChangedEvent(
        Change.CREATED, store.id, store.name, store.quantityProductsInStock);
  }

  public static StoreChangedEvent updated(Store store) {
    return new StoreChangedEvent(
        Change.UPDATED, store.id, store.name, store.quantityProductsInStock);
  }

  public Store toStore() {
    var store = new Store(name);
    store.id = id;
    store.quantityProductsInStock = quantityProductsInStock;

    return store;
  }
}
