package com.miw.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import org.apache.logging.log4j.*;

public class Dba {

	private static volatile boolean initialized = false;
	private static EntityManagerFactory emf = null;

	protected Logger logger = LogManager.getLogger(getClass());

	private EntityManager outer;

	public Dba() {
		this(false);
	}

	public Dba(boolean readOnly) {

		initialize();
		openEm(readOnly);
	}

	public void openEm(boolean readOnly) {
		if (outer != null) {
			return;
		}

		outer = emf.createEntityManager();

		if (readOnly == false) {
			outer.getTransaction().begin();
		}
	}

	// Obtiene la transacción activa (debe existir una transacción activa)
	public EntityManager getActiveEm() {
		if (outer == null) {
			throw new IllegalStateException("No transaction was active!");
		}

		return outer;
	}

	// Cierra el entity manager, haciendo commit o rollback si hay una transacción activa
	public void closeEm() {
		if (outer == null) {
			return;
		}

		try {
			if (outer.getTransaction().isActive()) {

				if (outer.getTransaction().getRollbackOnly()) {
					outer.getTransaction().rollback();
				} else {
					outer.getTransaction().commit();
				}
			}

		} finally {
			outer.close();
			outer = null;
		}
	}

	// Marca la transacción como rollback only, si hay una transacción activa
	public void markRollback() {

		if (outer != null) {
			outer.getTransaction().setRollbackOnly();
		}
	}

	public boolean isRollbackOnly() {
		return outer != null && outer.getTransaction().getRollbackOnly();
	}

	// thread safe way to initialize the entity manager factory.
	private void initialize() {

		if (initialized) {
			return;
		}

		synchronized (this) {

			if (initialized) {
				return;
			}

			initialized = true;

			try {
				emf = Persistence.createEntityManagerFactory("JPA_PU");

			} catch (Throwable t) {
				logger.error("Failed to setup persistence unit!", t);
				return;
			}
		}
	}
}