package com.veritynow.v2.txn.spi;

import java.util.List;

public record SagaPlan(boolean enabled, Mode mode, List<SagaStep> steps) {
    public enum Mode { NONE, COMPENSATE, TCC }

    public static SagaPlan none() {
        return new SagaPlan(false, Mode.NONE, List.of());
    }
}
