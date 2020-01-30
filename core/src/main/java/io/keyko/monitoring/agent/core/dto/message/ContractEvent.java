package io.keyko.monitoring.agent.core.dto.message;

import lombok.NoArgsConstructor;
import io.keyko.monitoring.agent.core.dto.event.ContractEventDetails;

@NoArgsConstructor
public class ContractEvent extends AbstractMessage<ContractEventDetails> {

    public static final String TYPE = "CONTRACT_EVENT";

    public ContractEvent(ContractEventDetails details) {
        super(details.getId(), TYPE, details);
    }
}