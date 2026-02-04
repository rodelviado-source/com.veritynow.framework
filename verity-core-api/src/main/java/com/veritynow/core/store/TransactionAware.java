package com.veritynow.core.store;

public interface TransactionAware<T> {
    void commit();
    void rollback();
    String begin();

	
}
