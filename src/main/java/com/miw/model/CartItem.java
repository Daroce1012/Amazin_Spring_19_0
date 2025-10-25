package com.miw.model;

public class CartItem {
    private Book book;
    private int quantity;
    
    public CartItem() {
        super();
    }
    
    public CartItem(Book book, int quantity) {
        super();
        this.book = book;
        this.quantity = quantity;
    }
    
    public Book getBook() {
        return book;
    }
    
    public void setBook(Book book) {
        this.book = book;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    
    public double getSubtotal() {
        return book.getPrice() * quantity;
    }
    
    // Métodos delegados para facilitar acceso en JSP
    public String getTitle() {
        return book.getTitle();
    }
    
    public int getBookId() {
        return book.getId();
    }
    
    public String getAuthor() {
        return book.getAuthor();
    }
    
    public double getUnitPrice() {
        return book.getPrice();
    }
    
    @Override
    public String toString() {
        return "CartItem [book=" + book + ", quantity=" + quantity + "]";
    }
}
