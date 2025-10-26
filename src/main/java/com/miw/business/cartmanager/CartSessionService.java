package com.miw.business.cartmanager;

import org.springframework.stereotype.Service;
import com.miw.model.Cart;
import jakarta.servlet.http.HttpSession;

@Service
public class CartSessionService {
    
    private static final String CART_ATTRIBUTE = "cart";
    
    // Obtiene el carrito de la sesión o crea uno nuevo si no existe
    public Cart getOrCreateCart(HttpSession session) {
        Cart cart = (Cart) session.getAttribute(CART_ATTRIBUTE);
        if (cart == null) {
            cart = new Cart();
            session.setAttribute(CART_ATTRIBUTE, cart);
        }
        return cart;
    }
    
    // Actualiza el carrito en la sesión
    public void updateCart(HttpSession session, Cart cart) {
        session.setAttribute(CART_ATTRIBUTE, cart);
    }
    
    // Elimina el carrito de la sesión
    public void clearCart(HttpSession session) {
        session.removeAttribute(CART_ATTRIBUTE);
    }
    
    // Elimina un item del carrito (reservado o no)
    public void removeItemFromCart(Cart cart, int bookId, boolean isReserved) {
        if (isReserved) {
            cart.removeReservedItem(bookId);
        } else {
            cart.removeNonReservedItem(bookId);
        }
    }
}

