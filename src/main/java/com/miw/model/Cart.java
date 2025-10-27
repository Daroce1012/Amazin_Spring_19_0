package com.miw.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Cart implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private List<CartItem> items;
    
    public Cart() {
        this.items = new ArrayList<>();
    }
    
    // Busca un item en el carrito por bookId y tipo (null busca cualquier tipo)
    private CartItem findItem(int bookId, Boolean isReserved) {
        for (CartItem item : items) {
            if (item.getBookId() == bookId) {
                // Si isReserved es null, retorna cualquier match
                // Si no, solo retorna si coincide el tipo
                if (isReserved == null || item.isReserved() == isReserved) {
                    return item;
                }
            }
        }
        return null;
    }
   
    // Añade un item al carrito, incrementa cantidad si ya existe
    public void addItem(Book book, int quantity, boolean isReserved) {
        CartItem existingItem = findItem(book.getId(), isReserved);
        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
        } else {
            items.add(new CartItem(book, quantity, isReserved));
        }
    }
    
    // Busca un item en el carrito por ID de libro y tipo
    public CartItem findItemByBookId(int bookId, boolean isReserved) {
        return findItem(bookId, isReserved);
    }
    
    // Elimina un item del carrito por ID de libro y tipo
    public void removeItem(int bookId, boolean isReserved) {
        items.removeIf(item -> item.getBookId() == bookId && item.isReserved() == isReserved);
    }
    
    // Elimina todos los items de un tipo (reservados o no)
    public void removeAllItems(boolean isReserved) {
        items.removeIf(item -> item.isReserved() == isReserved);
    }
    
    // Actualiza o añade un item reservado (cantidad absoluta, no incrementa)
    public void updateOrAddReservedItem(Book book, int quantity) {
        CartItem existingItem = findItem(book.getId(), true);
        if (existingItem != null) {
            existingItem.setQuantity(quantity);
        } else {
            items.add(new CartItem(book, quantity, true));
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
    
    public boolean isEmpty() {
        return items.isEmpty();
    }
    
    @Override
    public String toString() {
        return "Cart [items=" + items + "]";
    }
}
