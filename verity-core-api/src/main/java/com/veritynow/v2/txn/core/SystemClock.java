package com.veritynow.v2.txn.core;

import com.veritynow.v2.txn.spi.Clock;

public class SystemClock implements Clock {
    @Override
    public long nowMs() {
        return System.currentTimeMillis();
    }
}
