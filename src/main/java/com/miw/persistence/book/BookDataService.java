package com.miw.persistence.book;

import java.util.List;

import com.miw.model.Book;

public interface BookDataService {

	List<Book> getBooks() throws Exception;
	public Book newBook(Book book) throws Exception;
	public Book getBookById(int id) throws Exception;
	public void updateBookStock(int bookId, int newStock) throws Exception;
	public boolean checkStockAvailability(int bookId, int requestedQuantity) throws Exception;
	public boolean reduceStock(int bookId, int quantity) throws Exception;
}