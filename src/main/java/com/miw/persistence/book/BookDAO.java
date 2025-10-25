package com.miw.persistence.book;

import java.util.List;

import jakarta.persistence.EntityManager;

import org.apache.logging.log4j.*;

import com.miw.model.Book;
import com.miw.persistence.Dba;

public class BookDAO implements BookDataService  {

	protected Logger logger = LogManager.getLogger(getClass());

	public List<Book> getBooks() throws Exception {

		List<Book> resultList = null;

		Dba dba = new Dba();
		try {
			EntityManager em = dba.getActiveEm();

			resultList = em.createQuery("Select a From Book a", Book.class).getResultList();

			logger.debug("Result list: "+ resultList.toString());
			for (Book next : resultList) {
				logger.debug("next book: " + next);
			}

		} finally {
			// 100% sure that the transaction and entity manager will be closed
			dba.closeEm();
		}

		// We return the result
		return resultList;
	}

	public Book newBook(Book book) throws Exception {
		// TODO Auto-generated method stub

		Dba dba = new Dba();
		try {
			EntityManager em = dba.getActiveEm();
			em.persist(book);
			em.getTransaction().commit();

			logger.debug("Alta libro: "+ book.toString());

		} finally {
			// 100% sure that the transaction and entity manager will be closed
			dba.closeEm();
		}

		// We return the result
		return book;
	}
}