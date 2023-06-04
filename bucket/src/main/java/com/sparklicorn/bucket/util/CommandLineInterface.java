package com.sparklicorn.bucket.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class CommandLineInterface extends HashMap<String,String> {
    public static class ArgsMap extends HashMap<String,String> {}

    private Map<String, Consumer<ArgsMap>> commands;

    public CommandLineInterface() {
        this.commands = new HashMap<>();
    }

    /**
     * Registers the given command to the given function.
     * If the command was already registered, the function will be overwritten,
     * and the previous function returned.
     */
    public Consumer<ArgsMap> addCommand(String command, Consumer<ArgsMap> func) {
        return commands.put(command, func);
    }

    public Consumer<ArgsMap> removeCommand(String command) {
        return commands.remove(command);
    }
}
