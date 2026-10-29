package com.fulfilment.application.monolith.fulfilment.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.fulfilment.domain.models.FulfilmentUnit;
import com.fulfilment.application.monolith.fulfilment.domain.ports.FulfilmentUnitStore;
import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.stores.StoreRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.ws.rs.WebApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateFulfilmentUnitUseCaseTest {

  @Mock FulfilmentUnitStore fulfilmentUnitStore;

  @Mock WarehouseStore warehouseStore;

  @Mock StoreRepository storeRepository;

  @Mock ProductRepository productRepository;

  private CreateFulfilmentUnitUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase =
        new CreateFulfilmentUnitUseCase(
            fulfilmentUnitStore, warehouseStore, storeRepository, productRepository);
  }

  @Test
  void createsAssociationWhenAllRulesAreSatisfied() {
    FulfilmentUnit unit = unit(1L, 1L, 1L);
    unit.id = 99L;

    stubExistingEntities();
    when(fulfilmentUnitStore.exists(1L, 1L, 1L)).thenReturn(false);
    when(fulfilmentUnitStore.countDistinctWarehousesByStoreAndProduct(1L, 1L)).thenReturn(0L);
    when(fulfilmentUnitStore.existsByStoreAndWarehouse(1L, 1L)).thenReturn(false);
    when(fulfilmentUnitStore.countDistinctWarehousesByStore(1L)).thenReturn(0L);
    when(fulfilmentUnitStore.existsByWarehouseAndProduct(1L, 1L)).thenReturn(false);
    when(fulfilmentUnitStore.countDistinctProductsByWarehouse(1L)).thenReturn(0L);
    when(fulfilmentUnitStore.create(unit)).thenReturn(unit);

    FulfilmentUnit result = useCase.create(unit);

    assertSame(unit, result);
    assertNull(unit.id);
    verify(fulfilmentUnitStore).create(unit);
  }

  @Test
  void rejectsNullUnit() {
    assertStatus(422, () -> useCase.create(null));

    verify(fulfilmentUnitStore, never()).create(any());
  }

  @Test
  void rejectsMissingStoreId() {
    assertStatus(422, () -> useCase.create(unit(null, 1L, 1L)));

    verify(fulfilmentUnitStore, never()).create(any());
  }

  @Test
  void rejectsMissingProductId() {
    assertStatus(422, () -> useCase.create(unit(1L, null, 1L)));

    verify(fulfilmentUnitStore, never()).create(any());
  }

  @Test
  void rejectsMissingWarehouseId() {
    assertStatus(422, () -> useCase.create(unit(1L, 1L, null)));

    verify(fulfilmentUnitStore, never()).create(any());
  }

  @Test
  void rejectsUnknownStore() {
    when(storeRepository.findById(1L)).thenReturn(null);

    assertStatus(404, () -> useCase.create(unit(1L, 1L, 1L)));

    verify(fulfilmentUnitStore, never()).create(any());
  }

  @Test
  void rejectsUnknownProduct() {
    when(storeRepository.findById(1L)).thenReturn(new Store());
    when(productRepository.findById(1L)).thenReturn(null);

    assertStatus(404, () -> useCase.create(unit(1L, 1L, 1L)));

    verify(fulfilmentUnitStore, never()).create(any());
  }

  @Test
  void rejectsUnknownWarehouse() {
    when(storeRepository.findById(1L)).thenReturn(new Store());
    when(productRepository.findById(1L)).thenReturn(new Product());
    when(warehouseStore.findWarehouseById(1L)).thenReturn(null);

    assertStatus(404, () -> useCase.create(unit(1L, 1L, 1L)));

    verify(fulfilmentUnitStore, never()).create(any());
  }

  @Test
  void rejectsDuplicateAssociation() {
    stubExistingEntities();
    when(fulfilmentUnitStore.exists(1L, 1L, 1L)).thenReturn(true);

    assertStatus(422, () -> useCase.create(unit(1L, 1L, 1L)));

    verify(fulfilmentUnitStore, never()).create(any());
  }

  @Test
  void rejectsThirdWarehouseForProductInStore() {
    stubExistingEntities();
    when(fulfilmentUnitStore.exists(1L, 1L, 1L)).thenReturn(false);
    when(fulfilmentUnitStore.countDistinctWarehousesByStoreAndProduct(1L, 1L)).thenReturn(2L);

    assertStatus(422, () -> useCase.create(unit(1L, 1L, 1L)));

    verify(fulfilmentUnitStore, never()).create(any());
  }

  @Test
  void rejectsFourthWarehouseForStore() {
    stubExistingEntities();
    when(fulfilmentUnitStore.exists(1L, 1L, 1L)).thenReturn(false);
    when(fulfilmentUnitStore.countDistinctWarehousesByStoreAndProduct(1L, 1L)).thenReturn(0L);
    when(fulfilmentUnitStore.existsByStoreAndWarehouse(1L, 1L)).thenReturn(false);
    when(fulfilmentUnitStore.countDistinctWarehousesByStore(1L)).thenReturn(3L);

    assertStatus(422, () -> useCase.create(unit(1L, 1L, 1L)));

    verify(fulfilmentUnitStore, never()).create(any());
  }

  @Test
  void allowsReusingWarehouseAlreadyLinkedToStore() {
    FulfilmentUnit unit = unit(1L, 1L, 1L);
    stubExistingEntities();
    when(fulfilmentUnitStore.exists(1L, 1L, 1L)).thenReturn(false);
    when(fulfilmentUnitStore.countDistinctWarehousesByStoreAndProduct(1L, 1L)).thenReturn(0L);
    when(fulfilmentUnitStore.existsByStoreAndWarehouse(1L, 1L)).thenReturn(true);
    when(fulfilmentUnitStore.existsByWarehouseAndProduct(1L, 1L)).thenReturn(false);
    when(fulfilmentUnitStore.countDistinctProductsByWarehouse(1L)).thenReturn(0L);
    when(fulfilmentUnitStore.create(unit)).thenReturn(unit);

    useCase.create(unit);

    verify(fulfilmentUnitStore).create(unit);
  }

  @Test
  void rejectsSixthProductTypeForWarehouse() {
    stubExistingEntities();
    when(fulfilmentUnitStore.exists(1L, 1L, 1L)).thenReturn(false);
    when(fulfilmentUnitStore.countDistinctWarehousesByStoreAndProduct(1L, 1L)).thenReturn(0L);
    when(fulfilmentUnitStore.existsByStoreAndWarehouse(1L, 1L)).thenReturn(true);
    when(fulfilmentUnitStore.existsByWarehouseAndProduct(1L, 1L)).thenReturn(false);
    when(fulfilmentUnitStore.countDistinctProductsByWarehouse(1L)).thenReturn(5L);

    assertStatus(422, () -> useCase.create(unit(1L, 1L, 1L)));

    verify(fulfilmentUnitStore, never()).create(any());
  }

  @Test
  void allowsReusingProductAlreadyStoredByWarehouse() {
    FulfilmentUnit unit = unit(1L, 1L, 1L);
    stubExistingEntities();
    when(fulfilmentUnitStore.exists(1L, 1L, 1L)).thenReturn(false);
    when(fulfilmentUnitStore.countDistinctWarehousesByStoreAndProduct(1L, 1L)).thenReturn(0L);
    when(fulfilmentUnitStore.existsByStoreAndWarehouse(1L, 1L)).thenReturn(true);
    when(fulfilmentUnitStore.existsByWarehouseAndProduct(1L, 1L)).thenReturn(true);
    when(fulfilmentUnitStore.create(unit)).thenReturn(unit);

    useCase.create(unit);

    verify(fulfilmentUnitStore).create(unit);
  }

  private void stubExistingEntities() {
    when(storeRepository.findById(1L)).thenReturn(new Store());
    when(productRepository.findById(1L)).thenReturn(new Product());
    when(warehouseStore.findWarehouseById(1L)).thenReturn(new Warehouse());
  }

  private FulfilmentUnit unit(Long storeId, Long productId, Long warehouseId) {
    var unit = new FulfilmentUnit();
    unit.storeId = storeId;
    unit.productId = productId;
    unit.warehouseId = warehouseId;
    return unit;
  }

  private void assertStatus(int expectedStatus, Executable executable) {
    WebApplicationException exception =
        assertThrows(WebApplicationException.class, executable);
    assertEquals(expectedStatus, exception.getResponse().getStatus());
  }
}
