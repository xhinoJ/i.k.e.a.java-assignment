package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReplaceWarehouseUseCaseTest {

  @Mock WarehouseStore warehouseStore;

  @Mock LocationResolver locationResolver;

  private ReplaceWarehouseUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new ReplaceWarehouseUseCase(warehouseStore, locationResolver);
  }

  @Test
  void archivesPreviousWarehouseAndCreatesReplacement() {
    Warehouse previous = warehouse("MWH.001", "ZWOLLE-001", 100, 10);
    previous.id = 1L;
    Warehouse replacement = warehouse("MWH.001", "ZWOLLE-001", 20, 10);

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(previous);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001"))
        .thenReturn(new Location("ZWOLLE-001", 1, 40));
    when(warehouseStore.sumCapacityByLocation("ZWOLLE-001")).thenReturn(100L);

    useCase.replace(replacement);

    verify(warehouseStore).remove(previous);
    verify(warehouseStore).create(replacement);
  }

  @Test
  void rejectsReplacementWhenDestinationLocationIsFull() {
    Warehouse previous = warehouse("MWH.001", "ZWOLLE-001", 100, 10);
    Warehouse replacement = warehouse("MWH.001", "TILBURG-001", 20, 10);

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(previous);
    when(locationResolver.resolveByIdentifier("TILBURG-001"))
        .thenReturn(new Location("TILBURG-001", 1, 40));
    when(warehouseStore.sumCapacityByLocation("TILBURG-001")).thenReturn(30L);
    when(warehouseStore.countByLocation("TILBURG-001")).thenReturn(1L);

    assertStatus(422, () -> useCase.replace(replacement));

    verify(warehouseStore, never()).remove(any());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void rejectsReplacementWhenDestinationCapacityIsExceeded() {
    Warehouse previous = warehouse("MWH.001", "ZWOLLE-001", 100, 10);
    Warehouse replacement = warehouse("MWH.001", "TILBURG-001", 20, 10);

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(previous);
    when(locationResolver.resolveByIdentifier("TILBURG-001"))
        .thenReturn(new Location("TILBURG-001", 2, 40));
    when(warehouseStore.sumCapacityByLocation("TILBURG-001")).thenReturn(30L);
    when(warehouseStore.countByLocation("TILBURG-001")).thenReturn(0L);

    assertStatus(422, () -> useCase.replace(replacement));

    verify(warehouseStore, never()).remove(any());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void rejectsReplacementThatExceedsCapacityOfTheSameLocation() {
    Warehouse previous = warehouse("MWH.001", "ZWOLLE-002", 20, 5);
    Warehouse replacement = warehouse("MWH.001", "ZWOLLE-002", 60, 5);

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(previous);
    when(locationResolver.resolveByIdentifier("ZWOLLE-002"))
        .thenReturn(new Location("ZWOLLE-002", 2, 50));
    when(warehouseStore.sumCapacityByLocation("ZWOLLE-002")).thenReturn(20L);

    assertStatus(422, () -> useCase.replace(replacement));

    verify(warehouseStore, never()).remove(any());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void allowsReplacementThatFitsWithinTheSameLocationCapacity() {
    Warehouse previous = warehouse("MWH.001", "ZWOLLE-002", 20, 5);
    Warehouse replacement = warehouse("MWH.001", "ZWOLLE-002", 40, 5);

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(previous);
    when(locationResolver.resolveByIdentifier("ZWOLLE-002"))
        .thenReturn(new Location("ZWOLLE-002", 2, 50));
    when(warehouseStore.sumCapacityByLocation("ZWOLLE-002")).thenReturn(20L);

    useCase.replace(replacement);

    verify(warehouseStore).remove(previous);
    verify(warehouseStore).create(replacement);
  }

  @Test
  void allowsReplacementIntoLocationWithRoom() {
    Warehouse previous = warehouse("MWH.001", "ZWOLLE-001", 100, 10);
    Warehouse replacement = warehouse("MWH.001", "EINDHOVEN-001", 20, 10);

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(previous);
    when(locationResolver.resolveByIdentifier("EINDHOVEN-001"))
        .thenReturn(new Location("EINDHOVEN-001", 2, 70));
    when(warehouseStore.sumCapacityByLocation("EINDHOVEN-001")).thenReturn(10L);
    when(warehouseStore.countByLocation("EINDHOVEN-001")).thenReturn(0L);

    useCase.replace(replacement);

    verify(warehouseStore).remove(previous);
    verify(warehouseStore).create(replacement);
  }

  @Test
  void rejectsUnknownBusinessUnitCode() {
    Warehouse replacement = warehouse("MWH.UNKNOWN", "ZWOLLE-001", 20, 10);
    when(warehouseStore.findByBusinessUnitCode("MWH.UNKNOWN")).thenReturn(null);

    assertStatus(404, () -> useCase.replace(replacement));

    verify(warehouseStore, never()).remove(any());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void rejectsUnknownLocation() {
    Warehouse previous = warehouse("MWH.001", "ZWOLLE-001", 100, 10);
    Warehouse replacement = warehouse("MWH.001", "UNKNOWN-001", 20, 10);
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(previous);
    when(locationResolver.resolveByIdentifier("UNKNOWN-001")).thenReturn(null);

    assertStatus(422, () -> useCase.replace(replacement));

    verify(warehouseStore, never()).remove(any());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void rejectsCapacityThatCannotAccommodatePreviousStock() {
    Warehouse previous = warehouse("MWH.001", "ZWOLLE-001", 100, 10);
    Warehouse replacement = warehouse("MWH.001", "ZWOLLE-001", 8, 8);
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(previous);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001"))
        .thenReturn(new Location("ZWOLLE-001", 1, 40));

    assertStatus(422, () -> useCase.replace(replacement));

    verify(warehouseStore, never()).remove(any());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void rejectsStockThatDoesNotMatchPreviousStock() {
    Warehouse previous = warehouse("MWH.001", "ZWOLLE-001", 100, 10);
    Warehouse replacement = warehouse("MWH.001", "ZWOLLE-001", 20, 5);
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(previous);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001"))
        .thenReturn(new Location("ZWOLLE-001", 1, 40));

    assertStatus(422, () -> useCase.replace(replacement));

    verify(warehouseStore, never()).remove(any());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void rejectsStockGreaterThanCapacity() {
    Warehouse replacement = warehouse("MWH.001", "ZWOLLE-001", 10, 20);

    assertStatus(422, () -> useCase.replace(replacement));

    verify(warehouseStore, never()).remove(any());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void rejectsNonPositiveCapacity() {
    Warehouse replacement = warehouse("MWH.001", "ZWOLLE-001", 0, 0);

    assertStatus(422, () -> useCase.replace(replacement));

    verify(warehouseStore, never()).remove(any());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void rejectsNegativeStock() {
    Warehouse replacement = warehouse("MWH.001", "ZWOLLE-001", 10, -1);

    assertStatus(422, () -> useCase.replace(replacement));

    verify(warehouseStore, never()).remove(any());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void rejectsMissingBusinessUnitCode() {
    Warehouse replacement = warehouse(null, "ZWOLLE-001", 10, 5);

    assertStatus(422, () -> useCase.replace(replacement));

    verify(warehouseStore, never()).remove(any());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void rejectsMissingLocation() {
    Warehouse replacement = warehouse("MWH.001", " ", 10, 5);

    assertStatus(422, () -> useCase.replace(replacement));

    verify(warehouseStore, never()).remove(any());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void rejectsMissingCapacity() {
    Warehouse replacement = warehouse("MWH.001", "ZWOLLE-001", 10, 5);
    replacement.capacity = null;

    assertStatus(422, () -> useCase.replace(replacement));

    verify(warehouseStore, never()).remove(any());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void rejectsMissingStock() {
    Warehouse replacement = warehouse("MWH.001", "ZWOLLE-001", 10, 5);
    replacement.stock = null;

    assertStatus(422, () -> useCase.replace(replacement));

    verify(warehouseStore, never()).remove(any());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void rejectsNullWarehouse() {
    assertStatus(422, () -> useCase.replace(null));

    verify(warehouseStore, never()).remove(any());
    verify(warehouseStore, never()).create(any());
  }

  private Warehouse warehouse(String code, String location, int capacity, int stock) {
    var warehouse = new Warehouse();
    warehouse.businessUnitCode = code;
    warehouse.location = location;
    warehouse.capacity = capacity;
    warehouse.stock = stock;
    return warehouse;
  }

  private void assertStatus(int expectedStatus, Executable executable) {
    WebApplicationException exception =
        assertThrows(WebApplicationException.class, executable);
    assertEquals(expectedStatus, exception.getResponse().getStatus());
  }
}
