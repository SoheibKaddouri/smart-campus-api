/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.exception;

/**
 *
 * @author w2151373
 */

import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.*;
import java.util.Map;

@Provider
public class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {

    @Override
    public Response toResponse(LinkedResourceNotFoundException ex) {

        Map<String, Object> body = Map.of(
                "error", "The request contains a reference to a resource that does not exist.",
                "field", ex.getFieldName(),
                "invalidValue", ex.getInvalidValue(),
                "status", 422
        );

        return Response.status(422) // Unprocessable Entity
                .entity(body)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}

