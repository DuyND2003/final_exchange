package org.example.final_usth.matchingengine.snapshot;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class EngineState {
    private String id;
    private Long commandOffset;
    private Long messageOffset;
    private Long messageSequence;
    private Map<String, Long> tradeSequences = new HashMap<>();
    private Map<String, Long> orderSequences = new HashMap<>();
    private Map<String, Long> orderBookSequences = new HashMap<>();

}
