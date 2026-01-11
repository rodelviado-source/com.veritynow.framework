package com.veritynow.v2.txn.adapters.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

public class DbFencingTokenSequence implements FencingTokenProvider {

    @PersistenceContext
    private EntityManager em;

    public DbFencingTokenSequence(EntityManager em) {
        this.em = em;
    }

    @Override
    public long nextToken() {
        // Requires a DB sequence named vn_fencing_token_seq
        Object v = em.createNativeQuery("select nextval('vn_fencing_token_seq')").getSingleResult();
        if (v instanceof Number n) return n.longValue();
        return Long.parseLong(String.valueOf(v));
    }
}
