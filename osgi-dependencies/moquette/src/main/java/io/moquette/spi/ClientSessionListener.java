package io.moquette.spi;

import lombok.Value;

public interface ClientSessionListener {
    void onAcknowledged(SecondPhaseAcknowledged event);

    void onAcknowledged(FlightAcknowledged event);

    @Value
    class SecondPhaseAcknowledged {
        private final ClientSession session;

        private final String guid;
    }

    @Value
    class FlightAcknowledged {
        private final ClientSession session;

        private final String guid;
    }
}
