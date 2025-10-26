package com.miw.model;

import jakarta.persistence.*;

@Entity
public class Reservation {
    
    @Id 
    @GeneratedValue
    private int id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;
    
    private String username;
    private int quantity;
    
    // Constructor vacío para JPA
    public Reservation() {
        super();
    }
    
    // Constructor simplificado
    public Reservation(Book book, String username, int quantity) {
        this.book = book;
        this.username = username;
        this.quantity = quantity;
    }
    
    // Getters y Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public Book getBook() {
        return book;
    }
    
    public void setBook(Book book) {
        this.book = book;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    
    // Métodos calculados dinámicamente
    public double getReservationPrice() {
        return book.getPrice() * quantity * 0.05; // 5% pagado
    }
    
    public double getRemainingAmount() {
        return book.getPrice() * quantity * 0.95; // 95% restante
    }
    
    @Override
    public String toString() {
        return "Reservation [id=" + id + ", book=" + book.getTitle() + 
               ", username=" + username + ", quantity=" + quantity + "]";
    }
}
