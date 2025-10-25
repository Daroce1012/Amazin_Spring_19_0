package com.miw.model;

import java.util.ArrayList;
import java.util.List;

public class Cart {
    private List<CartItem> items;
    
    public Cart() {
        this.items = new ArrayList<>();
    }
    
    public void addItem(Book book, int quantity) {
        // Buscar si el libro ya está en el carrito
        for (CartItem item : items) {
            if (item.getBook().getId() == book.getId()) {
                // Si ya está, incrementar cantidad
                item.setQuantity(item.getQuantity() + quantity);
                return;
            }
        }
        
        // Si no está, crear nuevo CartItem
        items.add(new CartItem(book, quantity));
    }
    
    public void removeItem(int bookId) {
        items.removeIf(item -> item.getBook().getId() == bookId);
    }
    
    public void updateQuantity(int bookId, int newQuantity) {
        for (CartItem item : items) {
            if (item.getBook().getId() == bookId) {
                if (newQuantity <= 0) {
                    removeItem(bookId);
                } else {
                    item.setQuantity(newQuantity);
                }
                break;
            }
        }
    }
    
    public double getTotal() {
        return items.stream()
            .mapToDouble(CartItem::getSubtotal)
            .sum();
    }
    
    public List<CartItem> getItems() {
        return items;
    }
    
    public void setItems(List<CartItem> items) {
        this.items = items;
    }
    
    public void clear() {
        items.clear();
    }
    
    public int getTotalItems() {
        return items.stream()
            .mapToInt(CartItem::getQuantity)
            .sum();
    }
    
    public boolean isEmpty() {
        return items.isEmpty();
    }
    
    @Override
    public String toString() {
        return "Cart [items=" + items + "]";
    }
}
