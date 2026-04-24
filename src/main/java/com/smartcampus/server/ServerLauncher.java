/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.server;

/**
 *
 * @author w2151373
 */

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.jackson.JacksonFeature; // Add this import

import java.net.URI;

public class ServerLauncher {

    public static void main(String[] args) {
        // 1. Updated URI to include your api/v1 prefix
        final String BASE_URI = "http://localhost:8080/api/v1/";

        // 2. Register Jackson for JSON support
        ResourceConfig config = new ResourceConfig()
                .packages("com.smartcampus")
                .register(JacksonFeature.class); 

        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), config);

        System.out.println("SmartCampus API running at " + BASE_URI);
        System.out.println("Press Ctrl+C to stop...");
    }
}
