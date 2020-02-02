package io.keyko.monitoring.agent.core.chain.service.container;

import io.keyko.monitoring.agent.core.chain.service.BlockchainService;
import io.keyko.monitoring.agent.core.chain.service.Web3jService;
import lombok.Data;
import org.web3j.protocol.Web3j;

@Data
public class NodeServices {

    private String nodeName;

    private Web3j web3j;

    private Web3jService blockchainService;
}
