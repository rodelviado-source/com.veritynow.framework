package com.veritynow.v2.store;

import com.veritynow.context.ContextScope;

public interface TransactionAware {
	public void commit();
	public void rollback();
	public ContextScope begin();
}
