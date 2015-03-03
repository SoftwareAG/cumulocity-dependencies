package org.cometd.server;

import static org.cometd.server.SessionState.INACTIVE;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.Test;

public class ServerSessionImplTest {

    BayeuxServerImpl server = mock(BayeuxServerImpl.class);

    ServerSessionImpl session = new ServerSessionImpl(server);

    @Test
    public void shouldSwithToInactiveStateWhen() {
        //Given
        session.activate();
        //When
        session.setScheduler(null);
        //Then
        assertThat(session.getState()).isEqualTo(INACTIVE);
    }
    

}
