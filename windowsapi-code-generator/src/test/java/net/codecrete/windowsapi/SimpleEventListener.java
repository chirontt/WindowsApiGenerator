//
// Windows API Generator for Java
// Copyright (c) 2025 Manuel Bleichenbacher
// Licensed under MIT License
// https://opensource.org/licenses/MIT
//
package net.codecrete.windowsapi;

import net.codecrete.windowsapi.events.Event;
import net.codecrete.windowsapi.events.EventListener;

public class SimpleEventListener implements EventListener {

    @Override
    public void onEvent(Event event) {
        switch (event) {
            case Event.JavaSourceGenerated(var path) -> System.out.println("File generated: " + path);
            case Event.ConfigurationFileGenerated(var path) -> System.out.println("Configuration file generated: " + path);
            case Event.InvalidArgument(var ignored1, var ignored2, var reason) ->
                    System.out.println("Error: " + reason);
            case Event.DirectoryCreated(var path) -> System.out.println("Directory created: " + path);
            case Event.FileDeleted(var path) -> System.out.println("File deleted: " + path);
            case Event.DirectoryDeleted(var path) -> System.out.println("Directory deleted: " + path);
            default -> System.out.println("Unknown event: " + event);
        }
    }
}
