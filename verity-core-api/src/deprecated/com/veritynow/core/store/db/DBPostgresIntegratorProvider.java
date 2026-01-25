package com.veritynow.core.store.db;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.hibernate.jpa.boot.spi.JpaSettings;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "verity.db.aux-ddl.enabled", havingValue = "true", matchIfMissing = true)
public class DBPostgresIntegratorProvider implements IntegratorProvider, Integrator, HibernatePropertiesCustomizer  {

	@Override
	public List<Integrator> getIntegrators() {
		return  Collections.singletonList(this);
	}

	@Override
	public void integrate(Metadata metadata, BootstrapContext bootstrapContext,
			SessionFactoryImplementor sessionFactory) {
		
		metadata.getDatabase().addAuxiliaryDatabaseObject(new DBPostgresExtensionSupport());
		metadata.getDatabase().addAuxiliaryDatabaseObject(new DBPostgresLockingSupport());
		metadata.getDatabase().addAuxiliaryDatabaseObject(new DBPostgresTransactionSupport());
		
	}		 

	@Override
	public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		
	}

	
	@Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(JpaSettings.INTEGRATOR_PROVIDER, this );
    }
	
}
