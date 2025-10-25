
package com.miw.business.bookmanager;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.apache.logging.log4j.*;

import com.miw.model.Book;
import com.miw.persistence.book.BookDataService;
import com.miw.persistence.vat.VATDataService;

public class BookManager implements BookManagerService {
	Logger logger = LogManager.getLogger(this.getClass());
	
	@Autowired
	private BookDataService bookDataService;
	
	@Autowired
	private VATDataService ivaDataService;
	
	private Map<Integer, Integer> discounts;

	// Setter solo para discounts (se configura desde XML)
	public void setDiscounts(Map<Integer, Integer> discounts) {
		this.discounts = discounts;
	}

	/**
	 * Calcula el precio final con IVA y descuentos
	 */
	private double calculateFinalPrice(Book book) {
		return book.getBasePrice() 
			* (1 + book.getVat().getValue()) 
			* discounts.get(book.getVat().getTaxGroup());
	}

	public List<Book> getBooks() throws Exception {
		logger.debug("Asking for books");
		List<Book> books = bookDataService.getBooks();
		
		// We calculate the final price with the VAT value
		for (Book b : books) {
			b.setPrice(calculateFinalPrice(b));
		}
		return books;
	}
	
	public Book getSpecialOffer() throws Exception
	{
		List<Book> books = bookDataService.getBooks();
		int number = (int) (Math.random()*books.size());
		Book specialBook = books.get(number);
		logger.debug("Applying disccount to " + specialBook.getTitle());
		specialBook.setPrice(calculateFinalPrice(specialBook) * 0.85);
		return specialBook;
	}
	
	public Book newBook(Book book, int family) throws Exception {
		// TODO Auto-generated method stub
		book.setVat(this.ivaDataService.getVAT(family));
		return this.bookDataService.newBook(book);
	}
	
	@Override
	public Book getBookById(int id) throws Exception {
		logger.debug("Getting book by id: " + id);
		Book book = bookDataService.getBookById(id);
		
		if (book != null) {
			// Calculamos el precio con IVA y descuentos
			book.setPrice(calculateFinalPrice(book));
		}
		
		return book;
	}
	
	@Override
	public boolean checkStockAvailability(int bookId, int requestedQuantity) throws Exception {
		logger.debug("Checking stock for book " + bookId + ": " + requestedQuantity + " units");
		return bookDataService.checkStockAvailability(bookId, requestedQuantity);
	}
	
	@Override
	public boolean reduceStock(int bookId, int quantity) throws Exception {
		logger.debug("Reducing stock for book " + bookId + ": " + quantity + " units");
		return bookDataService.reduceStock(bookId, quantity);
	}
	
	@Override
	public boolean increaseStock(int bookId, int quantity) throws Exception {
		logger.debug("Increasing stock for book " + bookId + ": " + quantity + " units");
		
		try {
			// Llamar directamente al método atómico del DAO
			bookDataService.increaseBookStock(bookId, quantity);
			logger.debug("Stock increased successfully");
			return true;
		} catch (Exception e) {
			logger.error("Failed to increase stock for book " + bookId, e);
			return false;
		}
	}
}
