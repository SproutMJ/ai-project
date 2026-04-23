package org.mj.trip;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TripServiceTest {
    @Test
    public void testFormatTripName() {
        TripService tripService = new TripService();
        String result = tripService.formatTripName("Paris Trip");
        assertEquals("Trip: Paris Trip", result);
    }
}