package com.miw.business.cartmanager;

import com.miw.model.Cart;

public interface CartManagerService {
    void addBookToCart(Cart cart, int bookId, int quantity) throws Exception;
    Cart getCart();
    boolean checkout(Cart cart) throws Exception;
    double calculateTotal(Cart cart);
}
