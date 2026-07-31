package com.fulfilment.application.monolith.stores;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Pushes Store changes to the legacy system only once the transaction that produced them has been
 * committed, so the legacy system never sees data that we ended up rolling back.
 */
@ApplicationScoped
public class LegacyStoreSynchronizer {

  private static final Logger LOGGER = Logger.getLogger(LegacyStoreSynchronizer.class.getName());

  @Inject LegacyStoreManagerGateway legacyStoreManagerGateway;

  void onStoreChanged(
      @Observes(during = TransactionPhase.AFTER_SUCCESS) StoreChangedEvent event) {
    try {
      switch (event.change()) {
        case CREATED -> legacyStoreManagerGateway.createStoreOnLegacySystem(event.toStore());
        case UPDATED -> legacyStoreManagerGateway.updateStoreOnLegacySystem(event.toStore());
      }
    } catch (RuntimeException e) {
      // The data is already committed on our side, so a failure downstream must not bubble up into
      // the caller. Retrying it is out of the scope of this assignment.
      LOGGER.errorf(e, "Could not propagate the store %s to the legacy system", event.id());
    }
  }
}
