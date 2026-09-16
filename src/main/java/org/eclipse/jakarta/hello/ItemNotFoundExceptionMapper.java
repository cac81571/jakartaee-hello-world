package org.eclipse.jakarta.hello;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ItemNotFoundExceptionMapper implements ExceptionMapper<ItemNotFoundException> {

    @Override
    public Response toResponse(ItemNotFoundException exception) {
        return Response.status(Response.Status.NOT_FOUND)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorMessage(exception.getMessage()))
                .build();
    }

    public static class ErrorMessage {
        private final String error;

        public ErrorMessage(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }
    }
}
