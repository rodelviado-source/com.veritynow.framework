package com.veritynow.v2.store;

import java.util.Optional;

import com.veritynow.context.ContextScope;

public interface TransactionAware {
    void commit();
    void rollback();
    Optional<ContextScope> begin();
}
