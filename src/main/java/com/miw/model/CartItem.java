package com.miw.model;

public class CartItem {
    private Book book;
    private int quantity;
    private boolean isReserved; // Solo necesitamos esta marca
    
    public CartItem() {
        super();
    }
    
    public CartItem(Book book, int quantity) {
        super();
        this.book = book;
        this.quantity = quantity;
        this.isReserved = false;
    }
    
    public CartItem(Book book, int quantity, boolean isReserved) {
        super();
        this.book = book;
        this.quantity = quantity;
        this.isReserved = isReserved;
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
    
    public boolean isReserved() {
        return isReserved;
    }
    
    public void setReserved(boolean reserved) {
        isReserved = reserved;
    }
    
    // Subtotal considerando si es reserva (95% restante) o compra normal (100%)
    public double getSubtotal() {
        if (isReserved) {
            return book.getPrice() * quantity * 0.95; // 95% restante por pagar en reservas
        }
        return book.getPrice() * quantity;
    }
    
    // Calcula cuánto se pagó inicialmente (si es reserva)
    public double getPaidAmount() {
        if (isReserved) {
            return book.getPrice() * quantity * 0.05; // 5% ya pagado
        }
        return 0;
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
        return "CartItem [book=" + book + ", quantity=" + quantity + ", isReserved=" + isReserved + "]";
    }
}
