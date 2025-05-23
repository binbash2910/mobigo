package com.binbash.mobigo.domain;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class GroupMemberTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    public static GroupMember getGroupMemberSample1() {
        return new GroupMember().id(1L);
    }

    public static GroupMember getGroupMemberSample2() {
        return new GroupMember().id(2L);
    }

    public static GroupMember getGroupMemberRandomSampleGenerator() {
        return new GroupMember().id(longCount.incrementAndGet());
    }
}
