package org.example.final_usth.matchingengine.command;

import lombok.Getter;

@Getter
public enum CommandType {
    PLACE_ORDER((byte) 1),
    CANCEL_ORDER((byte) 2),
    DEPOSIT((byte) 3),
    WITHDRAWAL((byte) 4),
    PUT_PRODUCT((byte) 5);

    private final byte byteValue;

    CommandType(byte value) {
        this.byteValue = value;
    }

    public static CommandType valueOfByte(byte value) {
        for (CommandType commandType : CommandType.values()) {
            if (value == commandType.byteValue) {
                return commandType;
            }
        }
        throw new RuntimeException("Unknown command type" + value);
    }
}
