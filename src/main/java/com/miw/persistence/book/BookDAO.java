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

			logger.debug("New book added: "+ book.toString());

		} finally {
			// 100% sure that the transaction and entity manager will be closed
			dba.closeEm();
		}

		// We return the result
		return book;
	}

	public Book getBookById(int id) throws Exception {
		Book book = null;

		Dba dba = new Dba(true); // Solo lectura
		try {
			EntityManager em = dba.getActiveEm();
			book = em.find(Book.class, id);
			
			logger.debug("Book found: " + book);

		} finally {
			// 100% sure that the transaction and entity manager will be closed
			dba.closeEm();
		}

		return book;
	}

	@Override
	public void increaseBookStock(int bookId, int quantity) throws Exception {
		Dba dba = new Dba();
		try {
			EntityManager em = dba.getActiveEm();
			// Usar bloqueo pesimista para evitar condiciones de carrera
			Book book = em.find(Book.class, bookId, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
			
			if (book != null) {
				// Incrementar stock en una sola transacción atómica
				int newStock = book.getStock() + quantity;
				book.setStock(newStock);
				em.merge(book);
				
				logger.debug("Stock increased for book: " + book.getTitle() + " - Quantity added: " + quantity + " - New stock: " + newStock);
			} else {
				logger.error("Book with ID " + bookId + " not found");
				throw new Exception("Book with ID " + bookId + " not found");
			}

		} catch (Exception e) {
			logger.error("Error increasing stock for book " + bookId, e);
			throw e;
		} finally {
			dba.closeEm();
		}
	}
	
	@Override
	public boolean checkStockAvailability(int bookId, int requestedQuantity) throws Exception {
		Dba dba = new Dba(true); // Solo lectura
		try {
			EntityManager em = dba.getActiveEm();
			Book book = em.find(Book.class, bookId);
			
			if (book != null) {
				boolean available = book.getStock() >= requestedQuantity;
				logger.debug("Stock check for book " + bookId + ": Requested=" + requestedQuantity + ", Available=" + book.getStock() + ", Result=" + available);
				return available;
			}
			
			logger.error("Book with ID " + bookId + " not found");
			return false;

		} finally {
			// 100% sure that the transaction and entity manager will be closed
			dba.closeEm();
		}
	}
	
	@Override
	public boolean reduceStock(int bookId, int quantity) throws Exception {
		Dba dba = new Dba();
		try {
			EntityManager em = dba.getActiveEm();
			
			// BLOQUEO PESIMISTA para evitar condiciones de carrera
			Book book = em.find(Book.class, bookId, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
			
			if (book != null && book.getStock() >= quantity) {
				book.setStock(book.getStock() - quantity);
				em.merge(book);
				
				logger.debug("Stock reduced for book: " + book.getTitle() + " - Quantity reduced: " + quantity + " - New stock: " + book.getStock());
				return true;
			}
			
			if (book != null) {
				logger.error("Not enough stock for book " + bookId + ": Requested=" + quantity + ", Available=" + book.getStock());
			} else {
				logger.error("Book with ID " + bookId + " not found");
			}
			return false;

		} catch (Exception e) {
			logger.error("Error reducing stock for book " + bookId, e);
			throw e;
		} finally {
			// 100% sure that the transaction and entity manager will be closed
			dba.closeEm();
		}
	}
}