package com.fulfilment.application.monolith.stores;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Observes the same phase as the real synchronizer, so it only sees committed changes. */
@ApplicationScoped
public class LegacyStoreSyncRecorder {

  private final List<StoreChangedEvent> events = new CopyOnWriteArrayList<>();

  void onStoreChanged(@Observes(during = TransactionPhase.AFTER_SUCCESS) StoreChangedEvent event) {
    events.add(event);
  }

  public List<StoreChangedEvent> events() {
    return events;
  }

  public void clear() {
    events.clear();
  }
}
