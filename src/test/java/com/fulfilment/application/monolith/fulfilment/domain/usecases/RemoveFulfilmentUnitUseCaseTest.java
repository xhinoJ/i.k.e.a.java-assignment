package com.fulfilment.application.monolith.fulfilment.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.fulfilment.domain.models.FulfilmentUnit;
import com.fulfilment.application.monolith.fulfilment.domain.ports.FulfilmentUnitStore;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RemoveFulfilmentUnitUseCaseTest {

  @Mock FulfilmentUnitStore fulfilmentUnitStore;

  private RemoveFulfilmentUnitUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new RemoveFulfilmentUnitUseCase(fulfilmentUnitStore);
  }

  @Test
  void removesAssociation() {
    var unit = new FulfilmentUnit();
    unit.id = 1L;
    when(fulfilmentUnitStore.findFulfilmentUnitById(1L)).thenReturn(unit);

    useCase.remove(1L);

    verify(fulfilmentUnitStore).remove(unit);
  }

  @Test
  void rejectsUnknownAssociation() {
    when(fulfilmentUnitStore.findFulfilmentUnitById(1L)).thenReturn(null);

    assertStatus(404, () -> useCase.remove(1L));

    verify(fulfilmentUnitStore, never()).remove(any());
  }

  private void assertStatus(int expectedStatus, Executable executable) {
    WebApplicationException exception =
        assertThrows(WebApplicationException.class, executable);
    assertEquals(expectedStatus, exception.getResponse().getStatus());
  }
}
