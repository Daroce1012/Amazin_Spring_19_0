
package com.miw.business.bookmanager;

import java.util.List;

import org.apache.logging.log4j.*;

import com.miw.model.Book;


public class BookManagerEJBService implements BookManagerService {
	Logger logger = LogManager.getLogger(this.getClass());
	
	private Integer repetitions = 1;
	private BookManager bookManager=null;

	public BookManager getBookManager() {
		return bookManager;
	}

	public void setBookManager(BookManager bookManager) {
		this.bookManager = bookManager;
	}

	public Integer getRepetitions() {
		return repetitions;
	}

	public void setRepetitions(Integer repetitions) {
		this.repetitions = repetitions;
	}

	public List<Book> getBooks() throws Exception {
		printMessage("*** BookManagerEJBService.getBooks();");
		return bookManager.getBooks();
	}
	
	public Book getSpecialOffer() throws Exception
	{
		printMessage("*** BookManagerEJBService.getSpecialOffer();");
		return bookManager.getSpecialOffer();
	}
	
	private void printMessage(String message )
	{
		for ( int i = 0 ; i < repetitions ; i ++ )
		{
			System.out.println(message);
		}
	}
	
	public Book newBook(Book book, int family) throws Exception
	{
		return bookManager.newBook(book, family);
	}
}
