//package io.keyko.monitoring.agent.core.chain.contract;
//
//import ContractEventDetails;
//import BlockchainEventBroadcaster;
//import org.junit.Before;
//import org.junit.Test;
//
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.verify;
//
//public class BroadcastingEventListenerTest {
//
//    private BroadcastingEventListener underTest;
//
//    private BlockchainEventBroadcaster mockBroadcaster;
//
//    @Before
//    public void init() {
//        mockBroadcaster = mock(BlockchainEventBroadcaster.class);
//
//        underTest = new BroadcastingEventListener(mockBroadcaster);
//    }
//
//    @Test
//    public void testOnEvent() {
//        final ContractEventDetails contractEventDetails = new ContractEventDetails();
//        underTest.onEvent(contractEventDetails);
//
//        verify(mockBroadcaster).broadcastContractEvent(contractEventDetails);
//    }
//}
