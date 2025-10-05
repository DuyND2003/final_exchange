package org.example.final_usth.matchingengine.message;

import lombok.Getter;
import lombok.Setter;
import org.example.final_usth.matchingengine.entity.Account;

@Getter
@Setter
public class AccountMessage extends Message {
    private Account account;

    public AccountMessage() {
        this.setMessageType(MessageType.ACCOUNT);
    }
}

