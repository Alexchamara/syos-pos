package main.java.cli;

import java.util.logging.Logger;

/**
 * Base class for all command line interface commands
 */
public abstract class CliCommand {
    protected static final Logger LOGGER = Logger.getLogger(CliCommand.class.getName());

    /**
     * Execute the command
     */
    public abstract void execute(String[] args);

    /**
     * Get the command name
     */
    public abstract String getName();

    /**
     * Get help text for this command
     */
    public abstract String getHelp();
}
