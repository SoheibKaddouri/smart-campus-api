/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.api;

/**
 *
 * @author w2151373
 */

import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.net.URI;
import java.util.*;

public class SensorReadingResource {

    private final String sensorId;

    private static Map<String, List<SensorReading>> readings = new HashMap<>();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getReadings() {

        if (!SensorResource.sensorExists(sensorId)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Sensor not found"))
                    .build();
        }

        List<SensorReading> list = readings.getOrDefault(sensorId, new ArrayList<>());
        return Response.ok(list).build();
    }

@POST
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public Response addReading(SensorReading reading, @Context UriInfo uriInfo) {

    Sensor sensor = SensorResource.findSensor(sensorId);

    if (sensor == null) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", "Sensor not found"))
                .build();
    }

    if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
        throw new SensorUnavailableException(sensorId, sensor.getStatus());
    }

    readings.putIfAbsent(sensorId, new ArrayList<>());
    readings.get(sensorId).add(reading);

    sensor.setCurrentValue(reading.getValue());

    URI location = uriInfo.getAbsolutePathBuilder()
            .path(reading.getId())
            .build();

    return Response.created(location).entity(reading).build();
}

}
