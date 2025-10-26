package com.miw.model;

import java.util.ArrayList;
import java.util.List;

public class Cart {
    private List<CartItem> items;
    
    public Cart() {
        this.items = new ArrayList<>();
    }
    
    /**
     * Método privado para buscar un item en el carrito.
     * Centraliza la lógica de búsqueda para evitar código duplicado.
     * 
     * @param bookId El ID del libro a buscar
     * @param isReserved Si es null, busca cualquier tipo; si es true/false, filtra por tipo
     * @return El CartItem encontrado o null si no existe
     */
    private CartItem findItem(int bookId, Boolean isReserved) {
        for (CartItem item : items) {
            if (item.getBookId() == bookId) {
                // Si isReserved es null, no filtrar por tipo
                if (isReserved == null || item.isReserved() == isReserved) {
                    return item;
                }
            }
        }
        return null;
    }
    
    /**
     * Busca un item en el carrito por ID de libro (reserva o compra normal).
     * 
     * @param bookId El ID del libro a buscar
     * @return El CartItem encontrado o null si no existe
     */
    public CartItem findItemByBookId(int bookId) {
        return findItem(bookId, null);
    }
    
    /**
     * Busca un item reservado por ID de libro.
     * 
     * @param bookId El ID del libro a buscar
     * @return El CartItem reservado encontrado o null si no existe
     */
    public CartItem findReservedItemByBookId(int bookId) {
        return findItem(bookId, true);
    }
    
    /**
     * Busca un item de compra normal por ID de libro.
     * 
     * @param bookId El ID del libro a buscar
     * @return El CartItem de compra normal encontrado o null si no existe
     */
    public CartItem findNonReservedItemByBookId(int bookId) {
        return findItem(bookId, false);
    }
    
    /**
     * Método privado para añadir items al carrito, reutilizable para reservas y compras normales.
     * Evita duplicación de código entre addItem() y addReservedItem().
     * 
     * @param book El libro a añadir
     * @param quantity La cantidad a añadir (se suma si ya existe)
     * @param isReserved true si es una reserva, false si es compra normal
     */
    private void addItemInternal(Book book, int quantity, boolean isReserved) {
        CartItem existingItem = findItem(book.getId(), isReserved);
        if (existingItem != null) {
            // Si ya está con el mismo tipo, incrementar cantidad
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
        } else {
            // Si no está, crear nuevo CartItem
            items.add(new CartItem(book, quantity, isReserved));
        }
    }
    
    /**
     * Añade un item de compra normal al carrito.
     * Si ya existe como compra normal, incrementa la cantidad.
     */
    public void addItem(Book book, int quantity) {
        addItemInternal(book, quantity, false);
    }
    
    /**
     * Añade un item reservado al carrito.
     * Si ya existe una reserva del mismo libro, incrementa la cantidad.
     * 
     * @param book El libro a añadir
     * @param quantity La cantidad a añadir
     */
    public void addReservedItem(Book book, int quantity) {
        addItemInternal(book, quantity, true);
    }
    
    /**
     * Actualiza la cantidad de un item reservado si existe, o lo añade si no existe.
     * Útil cuando la cantidad es absoluta (no incremental), como al sincronizar con la BD.
     * 
     * @param book El libro a actualizar/añadir
     * @param quantity La cantidad absoluta a establecer
     */
    public void updateOrAddReservedItem(Book book, int quantity) {
        CartItem existingItem = findReservedItemByBookId(book.getId());
        if (existingItem != null) {
            // Si existe, actualizar cantidad (ABSOLUTA, no incremental)
            existingItem.setQuantity(quantity);
        } else {
            // Si no se encontró, añadirlo como reserva
            items.add(new CartItem(book, quantity, true));
        }
    }
    
    public void removeItem(int bookId) {
        items.removeIf(item -> item.getBookId() == bookId);
    }
    
    public void removeReservedItem(int bookId) {
        items.removeIf(item -> item.getBookId() == bookId && item.isReserved());
    }
    
    public void removeNonReservedItem(int bookId) {
        items.removeIf(item -> item.getBookId() == bookId && !item.isReserved());
    }
    
    public void updateQuantity(int bookId, int newQuantity) {
        CartItem item = findItemByBookId(bookId);
        if (item != null) {
            if (newQuantity <= 0) {
                removeItem(bookId);
            } else {
                item.setQuantity(newQuantity);
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
