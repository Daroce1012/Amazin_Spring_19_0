package com.miw.business.cartmanager;

import com.miw.model.Cart;

public interface CartManagerService {
    // Métodos públicos de la API
    void addBookToCart(Cart cart, int bookId, int quantity) throws Exception;
    void synchronizeCartForUser(String username, Cart cart) throws Exception;
    void removeItemFromCart(String username, Cart cart, int bookId) throws Exception;
    void clearCart(String username, Cart cart) throws Exception;
    void purchaseItem(String username, Cart cart, int bookId) throws Exception;
    boolean checkout(String username, Cart cart) throws Exception;
}
