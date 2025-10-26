package com.miw.model;

import java.util.ArrayList;
import java.util.List;

public class Cart {
    private List<CartItem> items;
    
    public Cart() {
        this.items = new ArrayList<>();
    }
    
    // MÉTODOS PRIVADOS AUXILIARES
    
    // Busca un item en el carrito por bookId y tipo (reservado o no)
    private CartItem findItem(int bookId, Boolean isReserved) {
        for (CartItem item : items) {
            if (item.getBookId() == bookId) {
                if (isReserved == null || item.isReserved() == isReserved) {
                    return item;
                }
            }
        }
        return null;
    }
    
    // Añade un item al carrito (reserva o compra normal), incrementa cantidad si ya existe
    private void addItemInternal(Book book, int quantity, boolean isReserved) {
        CartItem existingItem = findItem(book.getId(), isReserved);
        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
        } else {
            items.add(new CartItem(book, quantity, isReserved));
        }
    }
    
    // OPERACIONES DE COMPRA NORMAL
    
    // Añade un item de compra normal al carrito
    public void addItem(Book book, int quantity) {
        addItemInternal(book, quantity, false);
    }
    
    // Busca un item en el carrito por ID de libro
    public CartItem findItemByBookId(int bookId) {
        return findItem(bookId, null);
    }
    
    public void removeNonReservedItem(int bookId) {
        items.removeIf(item -> item.getBookId() == bookId && !item.isReserved());
    }
    
    // OPERACIONES DE RESERVA
    
    // Añade un item reservado al carrito
    public void addReservedItem(Book book, int quantity) {
        addItemInternal(book, quantity, true);
    }
    
    // Actualiza o añade un item reservado (cantidad absoluta)
    public void updateOrAddReservedItem(Book book, int quantity) {
        CartItem existingItem = findReservedItemByBookId(book.getId());
        if (existingItem != null) {
            existingItem.setQuantity(quantity);
        } else {
            items.add(new CartItem(book, quantity, true));
        }
    }
    
    // Busca un item reservado por ID de libro
    public CartItem findReservedItemByBookId(int bookId) {
        return findItem(bookId, true);
    }
    
    public void removeReservedItem(int bookId) {
        items.removeIf(item -> item.getBookId() == bookId && item.isReserved());
    }
    
    // OPERACIONES GENERALES
    
    public void removeItem(int bookId) {
        items.removeIf(item -> item.getBookId() == bookId);
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
    
    public boolean isEmpty() {
        return items.isEmpty();
    }
    
    @Override
    public String toString() {
        return "Cart [items=" + items + "]";
    }
}
