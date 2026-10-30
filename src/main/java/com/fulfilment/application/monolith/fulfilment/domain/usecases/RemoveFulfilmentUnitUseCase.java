package com.fulfilment.application.monolith.fulfilment.domain.usecases;

import com.fulfilment.application.monolith.fulfilment.domain.models.FulfilmentUnit;
import com.fulfilment.application.monolith.fulfilment.domain.ports.FulfilmentUnitStore;
import com.fulfilment.application.monolith.fulfilment.domain.ports.RemoveFulfilmentUnitOperation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import org.jboss.logging.Logger;

@ApplicationScoped
public class RemoveFulfilmentUnitUseCase implements RemoveFulfilmentUnitOperation {

  private static final Logger LOGGER = Logger.getLogger(RemoveFulfilmentUnitUseCase.class);

  private final FulfilmentUnitStore fulfilmentUnitStore;

  public RemoveFulfilmentUnitUseCase(FulfilmentUnitStore fulfilmentUnitStore) {
    this.fulfilmentUnitStore = fulfilmentUnitStore;
  }

  @Override
  @Transactional
  public void remove(Long id) {
    FulfilmentUnit unit = fulfilmentUnitStore.findFulfilmentUnitById(id);
    if (unit == null) {
      throw new WebApplicationException(
          "Fulfilment unit with id of " + id + " does not exist.", 404);
    }

    fulfilmentUnitStore.remove(unit);

    LOGGER.infof(
        "Removed fulfilment unit id=%d storeId=%d productId=%d warehouseId=%d",
        unit.id, unit.storeId, unit.productId, unit.warehouseId);
  }
}
