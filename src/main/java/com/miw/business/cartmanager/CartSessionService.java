package com.miw.business.cartmanager;

import org.springframework.stereotype.Service;
import com.miw.model.Cart;
import jakarta.servlet.http.HttpSession;

@Service
public class CartSessionService {
    
    private static final String CART_ATTRIBUTE = "cart";
    
    /**
     * Obtiene el carrito de la sesión o crea uno nuevo si no existe
     */
    public Cart getOrCreateCart(HttpSession session) {
        Cart cart = (Cart) session.getAttribute(CART_ATTRIBUTE);
        if (cart == null) {
            cart = new Cart();
            session.setAttribute(CART_ATTRIBUTE, cart);
        }
        return cart;
    }
    
    /**
     * Actualiza el carrito en la sesión
     */
    public void updateCart(HttpSession session, Cart cart) {
        session.setAttribute(CART_ATTRIBUTE, cart);
    }
    
    /**
     * Elimina el carrito de la sesión
     */
    public void clearCart(HttpSession session) {
        session.removeAttribute(CART_ATTRIBUTE);
    }
    
    /**
     * Elimina un item del carrito (reservado o no)
     */
    public void removeItemFromCart(Cart cart, int bookId, boolean isReserved) {
        if (isReserved) {
            cart.removeReservedItem(bookId);
        } else {
            cart.removeNonReservedItem(bookId);
        }
    }
    
    /**
     * Actualiza o añade un item reservado al carrito en sesión.
     * Obtiene el carrito, modifica y actualiza en sesión automáticamente.
     */
    public void updateReservedItemInCart(HttpSession session, com.miw.model.Book book, int quantity) {
        Cart cart = getOrCreateCart(session);
        cart.updateOrAddReservedItem(book, quantity);
        updateCart(session, cart);
    }
    
    /**
     * Añade un item reservado al carrito en sesión.
     * Obtiene el carrito, modifica y actualiza en sesión automáticamente.
     */
    public void addReservedItemToCart(HttpSession session, com.miw.model.Book book, int quantity) {
        Cart cart = getOrCreateCart(session);
        cart.addReservedItem(book, quantity);
        updateCart(session, cart);
    }
    
    /**
     * Elimina un item del carrito en sesión por ID de libro.
     * Obtiene el carrito, modifica y actualiza en sesión automáticamente.
     */
    public void removeItemFromCart(HttpSession session, int bookId) {
        Cart cart = getOrCreateCart(session);
        cart.removeItem(bookId);
        updateCart(session, cart);
    }
}

