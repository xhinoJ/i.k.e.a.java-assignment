package com.fulfilment.application.monolith.stores;

import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("store")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class StoreResource {

  @Inject StoreService storeService;
  @Inject LegacyStoreManagerGateway legacyStoreManagerGateway;

  @GET
  public List<Store> get() {
    return Store.listAll(Sort.by("name"));
  }

  @GET
  @Path("{id}")
  public Store getSingle(Long id) {
    Store entity = Store.findById(id);
    if (entity == null) {
      throw new WebApplicationException("Store with id of " + id + " does not exist.", 404);
    }
    return entity;
  }

  @POST
  public Response create(Store store) {
    Store created = storeService.create(store);

    // The service method is the transaction boundary: when it returns, the
    // store has been committed. Only then do we propagate it to the legacy system.
    legacyStoreManagerGateway.createStoreOnLegacySystem(created);

    return Response.ok(created).status(201).build();
  }

  @PUT
  @Path("{id}")
  public Store update(Long id, Store updatedStore) {
    Store entity = storeService.update(id, updatedStore);

    // Called after the service method (and therefore the transaction) completes.
    legacyStoreManagerGateway.updateStoreOnLegacySystem(updatedStore);

    return entity;
  }

  @PATCH
  @Path("{id}")
  public Store patch(Long id, Store updatedStore) {
    Store entity = storeService.patch(id, updatedStore);

    // Called after the service method (and therefore the transaction) completes.
    legacyStoreManagerGateway.updateStoreOnLegacySystem(updatedStore);

    return entity;
  }

  @DELETE
  @Path("{id}")
  public Response delete(Long id) {
    storeService.delete(id);
    return Response.status(204).build();
  }
}
