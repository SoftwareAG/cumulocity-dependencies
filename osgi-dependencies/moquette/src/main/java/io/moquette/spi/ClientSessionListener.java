package io.moquette.spi;

import io.moquette.spi.ClientSession.FlightAcknowledged;
import io.moquette.spi.ClientSession.SecondPhaseAcknowledged;

public interface ClientSessionListener {
    void onAcknowledged(SecondPhaseAcknowledged event);

    void onAcknowledged(FlightAcknowledged event);
}
