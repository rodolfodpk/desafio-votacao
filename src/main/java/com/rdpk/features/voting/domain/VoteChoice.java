package com.rdpk.features.voting.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum VoteChoice {
    YES("Yes"),
    NO("No");
    
    private final String value;
    
    VoteChoice(String value) {
        this.value = value;
    }
    
    @JsonValue
    public String getValue() {
        return value;
    }
    
    @JsonCreator
    public static VoteChoice fromValue(String value) {
        for (VoteChoice choice : VoteChoice.values()) {
            if (choice.value.equals(value)) {
                return choice;
            }
        }
        throw new IllegalArgumentException("Invalid vote value: " + value);
    }
}
