package com.binbash.mobigo.domain;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class BookingTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static Booking getBookingSample1() {
        return new Booking().id(1L).nbPlacesReservees(1L);
    }

    public static Booking getBookingSample2() {
        return new Booking().id(2L).nbPlacesReservees(2L);
    }

    public static Booking getBookingRandomSampleGenerator() {
        return new Booking().id(longCount.incrementAndGet()).nbPlacesReservees(longCount.incrementAndGet());
    }
}
