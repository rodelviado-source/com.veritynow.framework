package com.veritynow.core.store;

import java.util.Optional;

public interface TransactionAware<T> {
    void commit();
    void rollback();
    Optional<T> begin();
}
