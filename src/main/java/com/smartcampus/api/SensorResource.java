/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.api;

/**
 *
 * @author w2151373
 */
import com.smartcampus.model.Sensor;
import com.smartcampus.model.Room;
import com.smartcampus.exception.LinkedResourceNotFoundException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.net.URI;
import java.util.*;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    // In-memory storage for sensors
    private static Map<String, Sensor> sensors = new HashMap<>();

    @POST
    public Response createSensor(Sensor sensor, @Context UriInfo uriInfo) {

        // Validate room existence using custom exception
        if (sensor.getRoomId() == null || !RoomResource.roomExists(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException("roomId", sensor.getRoomId());
        }

        // Save sensor
        sensors.put(sensor.getId(), sensor);

        // Also link sensor to room
        Room room = RoomResource.findRoom(sensor.getRoomId());
        room.getSensorIds().add(sensor.getId());

        URI location = uriInfo.getAbsolutePathBuilder()
                .path(sensor.getId())
                .build();

        return Response.created(location).entity(sensor).build();
    }

    @GET
    public Response getAllSensors(@QueryParam("type") String type) {

        List<Sensor> result = new ArrayList<>(sensors.values());

        if (type != null && !type.isEmpty()) {
            result.removeIf(sensor -> !sensor.getType().equalsIgnoreCase(type));
        }

        return Response.ok(result).build();
    }

    public static boolean sensorExists(String id) {
        return sensors.containsKey(id);
    }

    public static Sensor findSensor(String id) {
        return sensors.get(id);
    }

    @Path("/{sensorId}/readings")
    public SensorReadingResource getSensorReadingResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }
}
