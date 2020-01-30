package io.keyko.monitoring.agent.core.server.integrationtest;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import static org.junit.Assert.assertEquals;

@TestPropertySource(locations="classpath:application-test-db.properties")
public class HttpServiceRestartRecoveryIT extends ServiceRestartRecoveryTests {

    @Test
    public void broadcastMissedBlocksOnStartupAfterFailureTest() throws Exception {
        doBroadcastMissedBlocksOnStartupAfterFailureTest();
    }

    @Test
    public void broadcastUnconfirmedEventAfterFailureTest() throws Exception {
        doBroadcastUnconfirmedEventAfterFailureTest();
    }

    @Test
    public void broadcastConfirmedEventAfter12BlocksWhenDownTest() throws Exception {
        doBroadcastConfirmedEventAfter12BlocksWhenDownTest();
    }

    @Test
    public void broadcastTransactionUnconfirmedAfterFailureTest() throws Exception {
        doBroadcastTransactionUnconfirmedAfterFailureTest();
    }

}
