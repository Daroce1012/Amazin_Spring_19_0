package com.miw.business.reservationmanager;

import java.util.List;
import com.miw.model.Reservation;
import com.miw.model.Cart;

public interface ReservationManagerService {
    // Operaciones de reserva
    Reservation reserveBook(String username, int bookId, int quantity) throws Exception;
    
    // Consultas
    List<Reservation> getReservations(String username) throws Exception;
    Reservation getReservationByUserAndBook(String username, int bookId) throws Exception;
    
    // Operaciones con validación de propiedad
    boolean purchaseReservation(int reservationId, String username) throws Exception;
    boolean cancelReservation(int reservationId, String username) throws Exception;
    boolean cancelReservationByUserAndBook(String username, int bookId) throws Exception;
    
    // Operaciones con carrito
    boolean processReservationsInCart(String username, Cart cart) throws Exception;
    void cancelAllReservationsInCart(String username, Cart cart) throws Exception;
}
