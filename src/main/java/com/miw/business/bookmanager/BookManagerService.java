package com.miw.business.bookmanager;

import java.util.List;

import com.miw.model.Book;

public interface BookManagerService {
	public List<Book> getBooks() throws Exception;
	public Book getSpecialOffer() throws Exception;
	public Book newBook(Book book, int family) throws Exception;
	
	// Métodos para el carrito de compra
	public Book getBookById(int id) throws Exception;
	public boolean checkStockAvailability(int bookId, int requestedQuantity) throws Exception;
	public boolean reduceStock(int bookId, int quantity) throws Exception;
}