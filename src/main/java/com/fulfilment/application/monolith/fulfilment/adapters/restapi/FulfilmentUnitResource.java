package com.fulfilment.application.monolith.fulfilment.adapters.restapi;

import com.fulfilment.application.monolith.fulfilment.domain.models.FulfilmentUnit;
import com.fulfilment.application.monolith.fulfilment.domain.ports.CreateFulfilmentUnitOperation;
import com.fulfilment.application.monolith.fulfilment.domain.ports.FulfilmentUnitStore;
import com.fulfilment.application.monolith.fulfilment.domain.ports.RemoveFulfilmentUnitOperation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("fulfilment-unit")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class FulfilmentUnitResource {

  @Inject FulfilmentUnitStore fulfilmentUnitStore;

  @Inject CreateFulfilmentUnitOperation createFulfilmentUnitOperation;

  @Inject RemoveFulfilmentUnitOperation removeFulfilmentUnitOperation;

  @GET
  public List<FulfilmentUnit> get() {
    return fulfilmentUnitStore.getAll();
  }

  @GET
  @Path("{id}")
  public FulfilmentUnit getSingle(Long id) {
    FulfilmentUnit unit = fulfilmentUnitStore.findFulfilmentUnitById(id);
    if (unit == null) {
      throw new WebApplicationException(
          "Fulfilment unit with id of " + id + " does not exist.", 404);
    }
    return unit;
  }

  @POST
  public Response create(FulfilmentUnit unit) {
    FulfilmentUnit created = createFulfilmentUnitOperation.create(unit);
    return Response.status(201).entity(created).build();
  }

  @DELETE
  @Path("{id}")
  public Response delete(Long id) {
    removeFulfilmentUnitOperation.remove(id);
    return Response.status(204).build();
  }
}
