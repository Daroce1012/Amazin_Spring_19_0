package com.miw.persistence.vat;

import java.util.List;

import jakarta.persistence.EntityManager;

import org.apache.logging.log4j.*;

import com.miw.model.VAT;
import com.miw.persistence.Dba;

public class VATDAO implements VATDataService  {

	protected Logger logger = LogManager.getLogger(getClass());

	public List<VAT> getVATs() throws Exception {

		List<VAT> resultList = null;

		Dba dba = new Dba();
		try {
			EntityManager em = dba.getActiveEm();

			resultList = em.createQuery("Select a From VAT a", VAT.class).getResultList();

			logger.debug("Result list: "+ resultList.toString());

		} finally {
			// 100% sure that the transaction and entity manager will be closed
			dba.closeEm();
		}

		// We return the result
		return resultList;
	}

	public VAT getVAT(int family) throws Exception {

		List<VAT> resultList = null;
		
		Dba dba = new Dba();
		try {
			EntityManager em = dba.getActiveEm();
			
			resultList = em.createQuery(
					"SELECT v FROM VAT v WHERE v.taxGroup = ?", VAT.class)
					.setParameter(1, family)
					.getResultList();

			logger.debug("Get VAT: "+ resultList.get(0).toString());

		} finally {
			// 100% sure that the transaction and entity manager will be closed
			dba.closeEm();
		}

		// We return the result
		return resultList.get(0);
	}
}