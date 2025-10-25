package com.miw.persistence.reservation;

import java.util.List;
import com.miw.model.Reservation;

public interface ReservationDataService {
    Reservation createReservation(String username, int bookId, int quantity) throws Exception;
    List<Reservation> getReservationsByUsername(String username) throws Exception;
    Reservation getReservationById(int id) throws Exception;
    void deleteReservation(int id) throws Exception;
    Reservation updateReservation(Reservation reservation) throws Exception;
}
