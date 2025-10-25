package com.miw.business.reservationmanager;

import java.util.List;
import com.miw.model.Reservation;

public interface ReservationManagerService {
    Reservation createReservation(String username, int bookId, int quantity) throws Exception;
    List<Reservation> getReservations(String username) throws Exception;
    Reservation getReservationByUserAndBook(String username, int bookId) throws Exception;
    boolean purchaseReservation(int reservationId) throws Exception;
    boolean cancelReservation(int reservationId) throws Exception;
    boolean cancelReservationByUserAndBook(String username, int bookId) throws Exception;
    Reservation incrementReservationQuantity(int reservationId, int additionalQuantity) throws Exception;
}
