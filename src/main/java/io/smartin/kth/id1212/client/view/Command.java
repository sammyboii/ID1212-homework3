package io.smartin.kth.id1212.client.view;

import io.smartin.kth.id1212.client.exceptions.ErroneousInputException;
import io.smartin.kth.id1212.client.exceptions.NoArgumentException;
import io.smartin.kth.id1212.client.exceptions.UnknownCommandException;

import java.util.Arrays;

public class Command {
    private final CommandType type;
    private final String[] args;

    private Command(CommandType type, String[] args) {
        this.type = type;
        this.args = args;
    }

    static Command parse(String cmd) throws UnknownCommandException, ErroneousInputException {
        String[] parts = cmd.split(" ");
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);
        String command = parts[0].toUpperCase();
        CommandType type;
        Command result;

        switch (command) {
            case "REGISTER":
                if (args.length != 2) {
                    throw new ErroneousInputException("usage: register username password");
                }
                result = new Command(CommandType.REGISTER, args); break;
            case "UNREGISTER":
                if (args.length != 0) {
                    throw new ErroneousInputException("usage: unregister");
                }
                result = new Command(CommandType.UNREGISTER, args); break;
            case "LOGIN":
                if (args.length != 2) {
                    throw new ErroneousInputException("usage: login username password");
                }
                result = new Command(CommandType.LOGIN, args); break;
            case "LOGOUT":
                if (args.length != 0) {
                    throw new ErroneousInputException("usage: logout");
                }
                result = new Command(CommandType.LOGOUT, args); break;
            case "UPLOAD":
                if (args.length != 3) {
                    throw new ErroneousInputException("usage: upload filename path [prw]");
                }
                result = new Command(CommandType.UPLOAD, args); break;
            case "DOWNLOAD":
                if (args.length != 1) {
                    throw new ErroneousInputException("usage: download filename");
                }
                result = new Command(CommandType.DOWNLOAD, args); break;
            case "LIST":
                if (args.length != 0) {
                    throw new ErroneousInputException("usage: list");
                }
                result = new Command(CommandType.LIST, args); break;
            case "PERMISSIONS":
                if (args.length != 2) {
                    throw new ErroneousInputException("usage: permissions filename [prw]");
                }
                result = new Command(CommandType.PERMISSIONS, args); break;
            case "SUBSCRIBE":
                if (args.length != 1) {
                    throw new ErroneousInputException("usage: subscribe filename");
                }
                result = new Command(CommandType.SUBSCRIBE, args); break;
            case "UNSUBSCRIBE":
                if (args.length != 1) {
                    throw new ErroneousInputException("usage: unsubscribe filename");
                }
                result = new Command(CommandType.UNSUBSCRIBE, args); break;
            case "DELETE":
                if (args.length != 1) {
                    throw new ErroneousInputException("usage: delete filename");
                }
                result = new Command(CommandType.DELETE, args); break;
            default: throw new UnknownCommandException("Unknown command '" + parts[0] + "'");
        }
        return result;
    }

    public CommandType getType() {
        return type;
    }

    public String getArg(int index) throws NoArgumentException {
        if (args.length <= index) {
            throw new NoArgumentException("No argument at index " + index);
        }
        return args[index];
    }

    public String[] getArgs() {
        return args;
    }

    public enum CommandType {
        REGISTER,
        UNREGISTER,
        LOGIN,
        LOGOUT,
        LIST,
        UPLOAD,
        DOWNLOAD,
        PERMISSIONS,
        SUBSCRIBE,
        UNSUBSCRIBE,
        DELETE,
        QUIT
    }
}
