package com.fulfilment.application.monolith.fulfilment.domain.ports;

import com.fulfilment.application.monolith.fulfilment.domain.models.FulfilmentUnit;

public interface CreateFulfilmentUnitOperation {

  FulfilmentUnit create(FulfilmentUnit unit);
}
