package io.keyko.monitoring.agent.core.chain.service;

import io.keyko.monitoring.agent.core.chain.block.BlockListener;
import io.keyko.monitoring.agent.core.chain.contract.ContractEventListener;
import io.keyko.monitoring.agent.core.chain.factory.ContractEventDetailsFactory;
import io.keyko.monitoring.agent.core.chain.service.domain.Block;
import io.keyko.monitoring.agent.core.chain.service.domain.wrapper.Web3jBlock;
import io.keyko.monitoring.agent.core.chain.service.domain.wrapper.Web3jTransactionReceipt;
import io.keyko.monitoring.agent.core.chain.service.strategy.BlockSubscriptionStrategy;
import io.keyko.monitoring.agent.core.chain.util.Web3jUtil;
import io.keyko.monitoring.agent.core.dto.event.filter.ContractEventFilter;
import io.keyko.monitoring.agent.core.dto.event.filter.ContractEventSpecification;
import io.keyko.monitoring.agent.core.model.EventFilterSubscription;
import io.keyko.monitoring.agent.core.service.AsyncTaskService;
import io.keyko.monitoring.agent.core.utils.ExecutorNameFactory;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.*;
import org.web3j.protocol.core.filters.FilterException;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.*;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * A BlockchainService implementating utilising the Web3j library.
 *
 * @author Craig Williams <craig.williams@consensys.net>
 */
@Slf4j
public class Web3jService implements BlockchainService {

    private static final String EVENT_EXECUTOR_NAME = "EVENT";

    @Value("${ethereum.client.address}")
    private String clientAddress;

    @Getter
    private String nodeName;

    @Getter
    @Setter
    private Web3j web3j;
    private ContractEventDetailsFactory eventDetailsFactory;
    private EventBlockManagementService blockManagement;
    private AsyncTaskService asyncTaskService;

    private BlockSubscriptionStrategy blockSubscriptionStrategy;

    public Web3jService(String nodeName,
                        Web3j web3j,
                        ContractEventDetailsFactory eventDetailsFactory,
                        EventBlockManagementService blockManagement,
                        BlockSubscriptionStrategy blockSubscriptionStrategy,
                        AsyncTaskService asyncTaskService) {
        this.nodeName = nodeName;
        this.web3j = web3j;
        this.eventDetailsFactory = eventDetailsFactory;
        this.blockManagement = blockManagement;
        this.blockSubscriptionStrategy = blockSubscriptionStrategy;
        this.asyncTaskService = asyncTaskService;
    }

    /**
     * {inheritDoc}
     */
    @Override
    public void addBlockListener(BlockListener blockListener) {
        blockSubscriptionStrategy.addBlockListener(blockListener);
    }

    /**
     * {inheritDoc}
     */
    @Override
    public void removeBlockListener(BlockListener blockListener) {
        blockSubscriptionStrategy.removeBlockListener(blockListener);
    }

    /**
     * {inheritDoc}
     */
    @Override
    public EventFilterSubscription registerEventListener(
            ContractEventFilter eventFilter, ContractEventListener eventListener) {
        log.debug("Registering event filter for event: {}", eventFilter.getId());
        final ContractEventSpecification eventSpec = eventFilter.getEventSpecification();

        final BigInteger startBlock = getStartBlockForEventFilter(eventFilter);

        EthFilter ethFilter = new EthFilter(
                new DefaultBlockParameterNumber(startBlock),
                DefaultBlockParameterName.LATEST, eventFilter.getContractAddress());

        if (eventFilter.getEventSpecification() != null) {
            ethFilter = ethFilter.addSingleTopic(Web3jUtil.getSignature(eventSpec));
        }

        final Flowable<Log> flowable = web3j.ethLogFlowable(ethFilter);

        final Disposable sub = flowable
//                .doOnError(error -> )
                .subscribe(theLog -> {
                    asyncTaskService.execute(ExecutorNameFactory.build(EVENT_EXECUTOR_NAME, eventFilter.getNode()), () -> {
                        log.debug("Dispatching log: {}", theLog);
                        eventListener.onEvent(
                                eventDetailsFactory.createEventDetails(eventFilter, theLog));
                    });
                }, error -> log.error("Flowable subscribe error: " + error.getMessage()));

        if (sub.isDisposed()) {
            //There was an error subscribing
            throw new BlockchainException(String.format(
                    "Failed to subcribe for filter %s.  The subscription is disposed.", eventFilter.getId()));
        }

        return new EventFilterSubscription(eventFilter, sub, startBlock);
    }


    /**
     * {inheritDoc}
     */
    @Override
    public void connect() {
        log.info("Subscribing to block events");
        blockSubscriptionStrategy.subscribe();
    }

    /**
     * {inheritDoc}
     */
    @Override
    public void disconnect() {
        log.info("Unsubscribing from block events");
        try {
            blockSubscriptionStrategy.unsubscribe();
        } catch (FilterException e) {
            log.warn("Unable to unregister block subscription.  " +
                    "This is probably because the node has restarted or we're in websocket mode");
        }
    }

    /**
     * {inheritDoc}
     */
    @Override
    public void reconnect() {
        log.info("Reconnecting...");
        disconnect();
        connect();
    }

    /**
     * {inheritDoc}
     */
    @Override
    public String getClientVersion() {
        try {
            final Web3ClientVersion web3ClientVersion = web3j.web3ClientVersion().send();
            return web3ClientVersion.getWeb3ClientVersion();
        } catch (IOException e) {
            throw new BlockchainException("Error when obtaining client version", e);
        }
    }

    /**
     * {inheritDoc}
     */
    @Override
    public io.keyko.monitoring.agent.core.chain.service.domain.TransactionReceipt getTransactionReceipt(String txId) {
        try {
            final EthGetTransactionReceipt response = web3j.ethGetTransactionReceipt(txId).send();

            return response
                    .getTransactionReceipt()
                    .map(receipt -> new Web3jTransactionReceipt(receipt))
                    .orElse(null);
        } catch (IOException e) {
            throw new BlockchainException("Unable to connect to the ethereum client", e);
        }
    }

    /**
     * {inheritDoc}
     */
    @Override
    public BigInteger getCurrentBlockNumber() {
        try {
            final EthBlockNumber ethBlockNumber = web3j.ethBlockNumber().send();

            return ethBlockNumber.getBlockNumber();
        } catch (IOException e) {
            throw new BlockchainException("Error when obtaining the current block number", e);
        }
    }

    public Optional<Block> getBlock(String blockHash, boolean fullTransactionObjects) {
        try {
            final EthBlock blockResponse = web3j.ethGetBlockByHash(blockHash, fullTransactionObjects).send();

            if (blockResponse.getBlock() == null) {
                return Optional.empty();
            }

            return Optional.of(new Web3jBlock(blockResponse.getBlock(), nodeName));
        } catch (IOException e) {
            throw new BlockchainException("Error when obtaining block with hash: " + blockHash, e);
        }

    }

    @Override
    public boolean isConnected() {
        return blockSubscriptionStrategy != null && blockSubscriptionStrategy.isSubscribed();
    }

    @Override
    public String getRevertReason(String from, String to, BigInteger blockNumber, String input) {
        try {
            return web3j.ethCall(
                    Transaction.createEthCallTransaction(from, to, input),
                    DefaultBlockParameter.valueOf(blockNumber)
            ).send().getRevertReason();
        } catch (IOException e) {
            throw new BlockchainException("Error getting the revert reason", e);
        }
    }

    @PreDestroy
    private void unregisterBlockSubscription() {
        blockSubscriptionStrategy.unsubscribe();
    }

    private BigInteger getStartBlockForEventFilter(ContractEventFilter filter) {
        return blockManagement.getLatestBlockForEvent(filter);
    }

    @Override
    public List<Type> executeReadCall(String contractAddress, Function function)    {

        try {
            EthCall response = web3j.ethCall(
                    Transaction.createEthCallTransaction(clientAddress, contractAddress, FunctionEncoder.encode(function)),
                    DefaultBlockParameterName.LATEST)
                    .sendAsync().get();
            log.info("EthCall " + response.getValue());
            return FunctionReturnDecoder.decode(
                    response.getValue(), function.getOutputParameters());

        } catch (ExecutionException | InterruptedException e)  {
            log.error("Unable to execute remote call " + e.getMessage());
        }
        return new ArrayList<>();
    }

}
