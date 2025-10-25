package com.miw.business.cartmanager;

import com.miw.model.Cart;
import java.util.List;
import com.miw.model.Reservation;

public interface CartManagerService {
    void addBookToCart(Cart cart, int bookId, int quantity) throws Exception;
    boolean reduceStockForPurchase(int bookId, int quantity) throws Exception;
    void synchronizeCartWithReservations(Cart cart, List<Reservation> reservations);
    boolean processNormalPurchases(Cart cart) throws Exception;
}
