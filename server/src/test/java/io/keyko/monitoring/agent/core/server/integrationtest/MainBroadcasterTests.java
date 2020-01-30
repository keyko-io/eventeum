package io.keyko.monitoring.agent.core.server.integrationtest;

import io.keyko.monitoring.agent.core.constant.Constants;
import io.keyko.monitoring.agent.core.dto.block.BlockDetails;
import io.keyko.monitoring.agent.core.dto.event.ContractEventDetails;
import io.keyko.monitoring.agent.core.dto.event.ContractEventStatus;
import io.keyko.monitoring.agent.core.dto.event.filter.ContractEventFilter;
import io.keyko.monitoring.agent.core.dto.transaction.TransactionDetails;
import io.keyko.monitoring.agent.core.dto.transaction.TransactionStatus;
import io.keyko.monitoring.agent.core.model.TransactionIdentifierType;
import io.keyko.monitoring.agent.core.model.TransactionMonitoringSpec;
import org.junit.Assert;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

public abstract class MainBroadcasterTests extends BaseKafkaIntegrationTest {

    public void doTestBroadcastsUnconfirmedEventAfterInitialEmit() throws Exception {

        final EventEmitter emitter = deployEventEmitterContract();

        final ContractEventFilter registeredFilter = registerDummyEventFilter(emitter.getContractAddress());
        emitter.emitEvent(stringToBytes("BytesValue"), BigInteger.TEN, "StringValue").send();

        waitForContractEventMessages(1);

        assertEquals(1, getBroadcastContractEvents().size());

        final ContractEventDetails eventDetails = getBroadcastContractEvents().get(0);
        verifyDummyEventDetails(registeredFilter, eventDetails, ContractEventStatus.UNCONFIRMED);
    }

    public void doTestBroadcastsNotOrderedEvent() throws Exception {
        final EventEmitter emitter = deployEventEmitterContract();

        final ContractEventFilter filter = createDummyEventNotOrderedFilter(emitter.getContractAddress());
        final ContractEventFilter registeredFilter = registerEventFilter(filter);
        emitter.emitEventNotOrdered(stringToBytes("BytesValue"), BigInteger.TEN, "StringValue").send();

        waitForContractEventMessages(1);

        assertEquals(1, getBroadcastContractEvents().size());

        final ContractEventDetails eventDetails = getBroadcastContractEvents().get(0);
        verifyDummyEventDetails(registeredFilter, eventDetails, ContractEventStatus.UNCONFIRMED);
    }

    public void doTestBroadcastsConfirmedEventAfterBlockThresholdReached() throws Exception {

        final EventEmitter emitter = deployEventEmitterContract();

        final ContractEventFilter registeredFilter = registerDummyEventFilter(emitter.getContractAddress());
        emitter.emitEvent(stringToBytes("BytesValue"), BigInteger.TEN, "StringValue").send();

        waitForFilterPoll();
        triggerBlocks(12);
        waitForContractEventMessages(2);

        assertEquals(2, getBroadcastContractEvents().size());

        final ContractEventDetails eventDetails = getBroadcastContractEvents().get(1);
        verifyDummyEventDetails(registeredFilter, eventDetails, ContractEventStatus.CONFIRMED);
    }

    public void doTestContractEventForUnregisteredEventFilterNotBroadcast() throws Exception {
        final EventEmitter emitter = deployEventEmitterContract();
        final ContractEventFilter filter = doRegisterAndUnregister(emitter.getContractAddress());
        emitter.emitEvent(stringToBytes("BytesValue"), BigInteger.TEN, "StringValue").send();

        waitForBroadcast();

        //For some reason events are sometimes consumed for old tests on circleci
        //Allow events as long as they aren't for this tests registered filter
        if (getBroadcastContractEvents().size() > 0) {
            getBroadcastContractEvents().forEach(
                    event -> assertNotEquals(filter.getId(), event.getFilterId()));
        }
    }

    public void doTestBroadcastBlock() throws Exception {
        triggerBlocks(1);

        waitForBlockMessages(1);

        Assert.assertTrue("No blocks received", getBroadcastBlockMessages().size() >= 1);

        BlockDetails blockDetails = getBroadcastBlockMessages().get(0);
        assertEquals(1, blockDetails.getNumber().compareTo(BigInteger.ZERO));
        assertNotNull(blockDetails.getHash());
    }

    public String doTestBroadcastsUnconfirmedTransactionAfterInitialMining() throws Exception {

        final String signedTxHex = createRawSignedTransactionHex();

        return monitorSendAndAssertTransactionBroadcastFilteredByHash(signedTxHex, TransactionStatus.UNCONFIRMED);
    }

    public void doTestBroadcastsConfirmedTransactionAfterBlockThresholdReached() throws Exception {

        final String txHash = doTestBroadcastsUnconfirmedTransactionAfterInitialMining();

        triggerBlocks(12);
        waitForTransactionMessages(2);

        assertEquals(2, getBroadcastTransactionMessages().size());
        final TransactionDetails txDetails = getBroadcastTransactionMessages().get(1);
        assertEquals(txHash, txDetails.getHash());
        assertEquals(TransactionStatus.CONFIRMED, txDetails.getStatus());
    }

    public String doTestBroadcastFailedTransactionFilteredByHash() throws Exception {

        final EventEmitter emitter = deployEventEmitterContract();

        //Sending ether to the emitter contract will fails as theres no payable fallback
        final String signedTxHex = createRawSignedTransactionHex(emitter.getContractAddress());

        return monitorSendAndAssertTransactionBroadcastFilteredByHash(signedTxHex, TransactionStatus.FAILED);
    }

    private String monitorSendAndAssertTransactionBroadcastFilteredByHash(
            String signedTxHex, TransactionStatus expectedStatus) throws ExecutionException, InterruptedException {

        final String txHash = Hash.sha3(signedTxHex);
        TransactionMonitoringSpec monitorSpec = new TransactionMonitoringSpec(TransactionIdentifierType.HASH, txHash, Constants.DEFAULT_NODE_NAME);

        monitorTransaction(monitorSpec);

        assertEquals(txHash, sendRawTransaction(signedTxHex));

        waitForTransactionMessages(1);

        assertEquals(1, getBroadcastTransactionMessages().size());

        final TransactionDetails txDetails = getBroadcastTransactionMessages().get(0);
        assertEquals(txHash, txDetails.getHash());
        assertEquals(expectedStatus, txDetails.getStatus());

        return txHash;
    }

    public String doTestBroadcastFailedTransactionFilteredByTo() throws Exception {

        final EventEmitter emitter = deployEventEmitterContract();

        String toAddress = emitter.getContractAddress();
        final String signedTxHex = createRawSignedTransactionHex(toAddress);

        return monitorSendAndAssertTransactionBroadcastByTo(signedTxHex, toAddress, TransactionStatus.FAILED);
    }

    private String monitorSendAndAssertTransactionBroadcastByTo(
            String signedTxHex, String toAddress, TransactionStatus expectedStatus) throws ExecutionException, InterruptedException {

        TransactionMonitoringSpec monitorSpec = new TransactionMonitoringSpec(TransactionIdentifierType.TO_ADDRESS, toAddress, Constants.DEFAULT_NODE_NAME);

        monitorTransaction(monitorSpec);

        sendRawTransaction(signedTxHex);

        waitForTransactionMessages(1);

        assertEquals(1, getBroadcastTransactionMessages().size());

        final TransactionDetails txDetails = getBroadcastTransactionMessages().get(0);
        assertEquals(Keys.toChecksumAddress(toAddress), txDetails.getTo());
        assertEquals(expectedStatus, txDetails.getStatus());

        return signedTxHex;
    }

    public String doTestBroadcastFailedTransactionFilteredByFrom() throws Exception {

        final EventEmitter emitter = deployEventEmitterContract();

        final String signedTxHex = createRawSignedTransactionHex(emitter.getContractAddress());

        return monitorSendAndAssertTransactionBroadcastByFrom(signedTxHex, TransactionStatus.FAILED);
    }

    private String monitorSendAndAssertTransactionBroadcastByFrom(
            String signedTxHex, TransactionStatus expectedStatus) throws ExecutionException, InterruptedException {

        TransactionMonitoringSpec monitorSpec = new TransactionMonitoringSpec(
                TransactionIdentifierType.FROM_ADDRESS,
                CREDS.getAddress(),
                Constants.DEFAULT_NODE_NAME,
                Arrays.asList(TransactionStatus.FAILED)
        );

        monitorTransaction(monitorSpec);

        sendRawTransaction(signedTxHex);

        waitForTransactionMessages(1);

        assertEquals(1, getBroadcastTransactionMessages().size());

        final TransactionDetails txDetails = getBroadcastTransactionMessages().get(0);
        assertEquals(Keys.toChecksumAddress(CREDS.getAddress()), txDetails.getFrom());
        assertEquals(expectedStatus, txDetails.getStatus());

        return signedTxHex;
    }
}
