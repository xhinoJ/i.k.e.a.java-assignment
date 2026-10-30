package com.fulfilment.application.monolith.stores;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import org.jboss.logging.Logger;

/**
 * Transactional boundary for {@link Store} persistence.
 *
 * <p>Keeping the database work in dedicated {@code @Transactional} methods means the transaction is
 * committed when each method returns. Callers can therefore run the legacy-system synchronisation
 * only after the store is guaranteed to be persisted.
 */
@ApplicationScoped
public class StoreService {

  private static final Logger LOGGER = Logger.getLogger(StoreService.class);

  @Transactional
  public Store create(Store store) {
    if (store.id != null) {
      throw new WebApplicationException("Id was invalidly set on request.", 422);
    }

    store.persist();

    LOGGER.infof("Created store id=%d name=%s", store.id, store.name);
    return store;
  }

  @Transactional
  public Store update(Long id, Store updatedStore) {
    if (updatedStore.name == null) {
      throw new WebApplicationException("Store Name was not set on request.", 422);
    }

    Store entity = Store.findById(id);
    if (entity == null) {
      throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
    }

    entity.name = updatedStore.name;
    entity.quantityProductsInStock = updatedStore.quantityProductsInStock;

    LOGGER.infof("Updated store id=%d name=%s", entity.id, entity.name);
    return entity;
  }

  @Transactional
  public Store patch(Long id, StorePatchRequest request) {
    if (request == null || request.name == null) {
      throw new WebApplicationException("Store Name was not set on request.", 422);
    }

    Store entity = Store.findById(id);
    if (entity == null) {
      throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
    }

    entity.name = request.name;

    // null means "not provided": leave the stored value untouched. An explicit 0 is a valid
    // update, which is why the request field is a nullable Integer instead of a primitive int.
    if (request.quantityProductsInStock != null) {
      entity.quantityProductsInStock = request.quantityProductsInStock;
    }

    LOGGER.infof("Patched store id=%d name=%s", entity.id, entity.name);
    return entity;
  }

  @Transactional
  public void delete(Long id) {
    Store entity = Store.findById(id);
    if (entity == null) {
      throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
    }
    entity.delete();

    LOGGER.infof("Deleted store id=%d name=%s", entity.id, entity.name);
  }
}
