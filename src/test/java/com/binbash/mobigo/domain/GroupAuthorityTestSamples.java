package com.binbash.mobigo.domain;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class GroupAuthorityTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static GroupAuthority getGroupAuthoritySample1() {
        return new GroupAuthority().id(1L);
    }

    public static GroupAuthority getGroupAuthoritySample2() {
        return new GroupAuthority().id(2L);
    }

    public static GroupAuthority getGroupAuthorityRandomSampleGenerator() {
        return new GroupAuthority().id(longCount.incrementAndGet());
    }
}
