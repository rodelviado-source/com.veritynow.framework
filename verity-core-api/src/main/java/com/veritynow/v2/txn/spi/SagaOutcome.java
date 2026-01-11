package com.veritynow.v2.txn.spi;

public record SagaOutcome(boolean ok, String reason) {
    public static SagaOutcome isOk() { return new SagaOutcome(true, null); }
    public static SagaOutcome fail(String reason) { return new SagaOutcome(false, reason); }
}
