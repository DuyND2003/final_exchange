package org.example.final_usth.matchingengine.message;

import lombok.Getter;
import lombok.Setter;
import org.example.final_usth.matchingengine.command.Command;

@Getter
@Setter
public class CommandStartMessage extends Message {
    private Command command;
    private long commandOffset;

    public CommandStartMessage() {
        this.setMessageType(MessageType.COMMAND_START);
    }
}

