package com.veritynow.v2.txn.adapters.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaTxnRepository extends JpaRepository<TxnEntity, String> {}
