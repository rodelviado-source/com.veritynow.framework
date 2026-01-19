package com.veritynow.core.store;

import java.util.Optional;

import com.veritynow.core.context.ContextScope;

public interface TransactionAware {
    void commit();
    void rollback();
    Optional<ContextScope> begin();
}
