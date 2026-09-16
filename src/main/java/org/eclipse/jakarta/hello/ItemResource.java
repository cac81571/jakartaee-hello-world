package org.eclipse.jakarta.hello;

import java.net.URI;
import java.util.List;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("items")
@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ItemResource {

    @Inject
    private ItemService items;

    @GET
    public List<Item> list() {
        return items.findAll();
    }

    @GET
    @Path("{id}")
    public Item get(@PathParam("id") Long id) {
        return items.find(id);
    }

    @POST
    public Response create(Item item) {
        requireId(item);
        requireName(item);
        Item created = items.create(item);
        return Response.created(URI.create("items/" + created.getId())).entity(created).build();
    }

    @PUT
    @Path("{id}")
    public Item update(@PathParam("id") Long id, Item item) {
        requireName(item);
        return items.update(id, item);
    }

    @DELETE
    @Path("{id}")
    public Response delete(@PathParam("id") Long id) {
        items.delete(id);
        return Response.noContent().build();
    }

    private void requireId(Item item) {
        if (item == null || item.getId() == null) {
            throw new BadRequestException("id is required");
        }
    }

    private void requireName(Item item) {
        if (item == null || item.getName() == null || item.getName().isBlank()) {
            throw new BadRequestException("name is required");
        }
    }
}
