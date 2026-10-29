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
class CreateWarehouseUseCaseTest {

  @Mock WarehouseStore warehouseStore;

  @Mock LocationResolver locationResolver;

  private CreateWarehouseUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new CreateWarehouseUseCase(warehouseStore, locationResolver);
  }

  @Test
  void createsWarehouseWhenAllRulesAreSatisfied() {
    Warehouse warehouse = warehouse("MWH.NEW", "ZWOLLE-001", 30, 10);
    when(warehouseStore.findByBusinessUnitCode("MWH.NEW")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001"))
        .thenReturn(new Location("ZWOLLE-001", 1, 40));
    when(warehouseStore.countByLocation("ZWOLLE-001")).thenReturn(0L);
    when(warehouseStore.sumCapacityByLocation("ZWOLLE-001")).thenReturn(0L);

    useCase.create(warehouse);

    verify(warehouseStore).create(warehouse);
  }

  @Test
  void storesCanonicalLocationIdentifier() {
    Warehouse warehouse = warehouse("MWH.NEW", "zwolle-001", 30, 10);
    when(warehouseStore.findByBusinessUnitCode("MWH.NEW")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("zwolle-001"))
        .thenReturn(new Location("ZWOLLE-001", 1, 40));
    when(warehouseStore.countByLocation("ZWOLLE-001")).thenReturn(0L);
    when(warehouseStore.sumCapacityByLocation("ZWOLLE-001")).thenReturn(0L);

    useCase.create(warehouse);

    assertEquals("ZWOLLE-001", warehouse.location);
    verify(warehouseStore).create(warehouse);
  }

  @Test
  void rejectsDuplicateBusinessUnitCode() {
    Warehouse warehouse = warehouse("MWH.001", "ZWOLLE-001", 30, 10);
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(new Warehouse());

    assertStatus(422, () -> useCase.create(warehouse));

    verify(warehouseStore, never()).create(any());
  }

  @Test
  void rejectsUnknownLocation() {
    Warehouse warehouse = warehouse("MWH.NEW", "UNKNOWN-001", 30, 10);
    when(warehouseStore.findByBusinessUnitCode("MWH.NEW")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("UNKNOWN-001")).thenReturn(null);

    assertStatus(422, () -> useCase.create(warehouse));

    verify(warehouseStore, never()).create(any());
  }

  @Test
  void rejectsWhenLocationAlreadyHasMaximumNumberOfWarehouses() {
    Warehouse warehouse = warehouse("MWH.NEW", "ZWOLLE-001", 5, 1);
    when(warehouseStore.findByBusinessUnitCode("MWH.NEW")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001"))
        .thenReturn(new Location("ZWOLLE-001", 1, 40));
    when(warehouseStore.countByLocation("ZWOLLE-001")).thenReturn(1L);

    assertStatus(422, () -> useCase.create(warehouse));

    verify(warehouseStore, never()).create(any());
  }

  @Test
  void rejectsWhenLocationCapacityWouldBeExceeded() {
    Warehouse warehouse = warehouse("MWH.NEW", "ZWOLLE-001", 20, 5);
    when(warehouseStore.findByBusinessUnitCode("MWH.NEW")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001"))
        .thenReturn(new Location("ZWOLLE-001", 2, 40));
    when(warehouseStore.countByLocation("ZWOLLE-001")).thenReturn(0L);
    when(warehouseStore.sumCapacityByLocation("ZWOLLE-001")).thenReturn(30L);

    assertStatus(422, () -> useCase.create(warehouse));

    verify(warehouseStore, never()).create(any());
  }

  @Test
  void rejectsStockGreaterThanCapacity() {
    Warehouse warehouse = warehouse("MWH.NEW", "ZWOLLE-001", 10, 20);

    assertStatus(422, () -> useCase.create(warehouse));

    verify(warehouseStore, never()).create(any());
  }

  @Test
  void rejectsNonPositiveCapacity() {
    Warehouse warehouse = warehouse("MWH.NEW", "ZWOLLE-001", 0, 0);

    assertStatus(422, () -> useCase.create(warehouse));

    verify(warehouseStore, never()).create(any());
  }

  @Test
  void rejectsNegativeStock() {
    Warehouse warehouse = warehouse("MWH.NEW", "ZWOLLE-001", 10, -1);

    assertStatus(422, () -> useCase.create(warehouse));

    verify(warehouseStore, never()).create(any());
  }

  @Test
  void rejectsMissingBusinessUnitCode() {
    Warehouse warehouse = warehouse(" ", "ZWOLLE-001", 10, 5);

    assertStatus(422, () -> useCase.create(warehouse));

    verify(warehouseStore, never()).create(any());
  }

  @Test
  void rejectsMissingLocation() {
    Warehouse warehouse = warehouse("MWH.NEW", null, 10, 5);

    assertStatus(422, () -> useCase.create(warehouse));

    verify(warehouseStore, never()).create(any());
  }

  @Test
  void rejectsMissingCapacity() {
    Warehouse warehouse = warehouse("MWH.NEW", "ZWOLLE-001", 10, 5);
    warehouse.capacity = null;

    assertStatus(422, () -> useCase.create(warehouse));

    verify(warehouseStore, never()).create(any());
  }

  @Test
  void rejectsMissingStock() {
    Warehouse warehouse = warehouse("MWH.NEW", "ZWOLLE-001", 10, 5);
    warehouse.stock = null;

    assertStatus(422, () -> useCase.create(warehouse));

    verify(warehouseStore, never()).create(any());
  }

  @Test
  void rejectsNullWarehouse() {
    assertStatus(422, () -> useCase.create(null));

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
