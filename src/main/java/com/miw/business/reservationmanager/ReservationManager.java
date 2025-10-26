package com.miw.business.reservationmanager;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.logging.log4j.*;
import com.miw.business.bookmanager.BookManagerService;
import com.miw.model.Book;
import com.miw.model.Cart;
import com.miw.model.CartItem;
import com.miw.model.Reservation;
import com.miw.persistence.reservation.ReservationDataService;

public class ReservationManager implements ReservationManagerService {
    
    private static final Logger logger = LogManager.getLogger(ReservationManager.class);
    
    @Autowired
    private ReservationDataService reservationDataService;
    
    @Autowired
    private BookManagerService bookManagerService;
    
    // MÉTODOS PRIVADOS AUXILIARES
    
    // Actualiza el precio del libro en una reserva
    private void updateBookPrice(Reservation reservation) throws Exception {
        Book book = reservation.getBook();
        if (book != null) {
            Book bookWithPrice = bookManagerService.getBookById(book.getId());
            if (bookWithPrice != null) {
                book.setPrice(bookWithPrice.getPrice());
            }
        }
    }
    
    private Reservation getReservationById(int reservationId, String username) throws Exception {
        logger.debug("Getting reservation by ID: {} for user: {}", reservationId, username);
        
        Reservation reservation = reservationDataService.getReservationById(reservationId);
        
        if (reservation == null) {
            throw new Exception("reservation.notFound");
        }
        
        if (!reservation.getUsername().equals(username)) {
            logger.warn("User {} tried to access reservation {} belonging to {}", 
                       username, reservationId, reservation.getUsername());
            throw new Exception("reservation.accessDenied");
        }
        
        updateBookPrice(reservation);
        return reservation;
    }
    
    // Compra reserva SIN validar usuario (para uso interno)
    private boolean purchaseReservationInternal(int reservationId) throws Exception {
        logger.debug("Purchasing reservation: {}", reservationId);
        
        Reservation reservation = reservationDataService.getReservationById(reservationId);
        
        if (reservation == null) {
            throw new Exception("reservation.notFound");
        }
        
        reservationDataService.deleteReservation(reservationId);
        logger.debug("Reservation purchased and deleted. Stock remains reduced.");
        return true;
    }
    
    // Cancela reserva SIN validar usuario (para uso interno)
    private boolean cancelReservationInternal(int reservationId) throws Exception {
        logger.debug("Cancelling reservation: {}", reservationId);
        
        Reservation reservation = reservationDataService.getReservationById(reservationId);
        
        if (reservation == null) {
            throw new Exception("reservation.notFound");
        }
        
        Book book = reservation.getBook();
        bookManagerService.increaseStock(book.getId(), reservation.getQuantity());
        logger.debug("Stock restored for book {}", book.getId());
        
        reservationDataService.deleteReservation(reservationId);
        logger.debug("Reservation cancelled and deleted");
        return true;
    }
    
    // MÉTODOS PÚBLICOS DE LA API
    
    @Override
    public List<Reservation> getReservations(String username) throws Exception {
        logger.debug("Getting reservations for user: {}", username);
        List<Reservation> reservations = reservationDataService.getReservationsByUsername(username);
        
        for (Reservation reservation : reservations) {
            updateBookPrice(reservation);
        }
        
        return reservations;
    }
    
    @Override
    public Reservation getReservationByUserAndBook(String username, int bookId) throws Exception {
        logger.debug("Getting reservation for user={}, bookId={}", username, bookId);
        List<Reservation> reservations = getReservations(username);
        return reservations.stream()
            .filter(r -> r.getBook().getId() == bookId)
            .findFirst()
            .orElse(null);
    }
    
    @Override
    public boolean purchaseReservation(int reservationId, String username) throws Exception {
        logger.debug("Purchasing reservation: {} for user: {}", reservationId, username);
        
        getReservationById(reservationId, username);
        return purchaseReservationInternal(reservationId);
    }
    
    @Override
    public boolean cancelReservation(int reservationId, String username) throws Exception {
        logger.debug("Cancelling reservation: {} for user: {}", reservationId, username);
        
        getReservationById(reservationId, username);
        return cancelReservationInternal(reservationId);
    }
    
    @Override
    public boolean cancelReservationByUserAndBook(String username, int bookId) throws Exception {
        logger.debug("Cancelling reservation for user={}, bookId={}", username, bookId);
        
        Reservation reservation = getReservationByUserAndBook(username, bookId);
        
        if (reservation == null) {
            logger.warn("No reservation found for user={}, bookId={}", username, bookId);
            return false;
        }
        
        return cancelReservationInternal(reservation.getId());
    }
    
    @Override
    public boolean processReservationsInCart(String username, Cart cart) throws Exception {
        logger.debug("Processing reservations in cart for user: {}", username);
        
        for (CartItem item : cart.getItems()) {
            if (item.isReserved()) {
                Reservation res = getReservationByUserAndBook(username, item.getBookId());
                if (res != null) {
                    purchaseReservationInternal(res.getId());
                }
            }
        }
        
        return true;
    }
    
    @Override
    public void cancelAllReservationsInCart(String username, Cart cart) throws Exception {
        logger.debug("Cancelling all reservations in cart for user: {}", username);
        
        for (CartItem item : cart.getItems()) {
            if (item.isReserved()) {
                boolean cancelled = cancelReservationByUserAndBook(username, item.getBookId());
                if (!cancelled) {
                    logger.warn("Could not cancel reservation for bookId: {}", item.getBookId());
                }
            }
        }
    }
    
    @Override
    public Reservation reserveBook(String username, int bookId, int quantity) throws Exception {
        logger.debug("Reserve book request: user={}, bookId={}, quantity={}", username, bookId, quantity);
        
        // Verificar si ya existe una reserva
        Reservation existingReservation = getReservationByUserAndBook(username, bookId);
        boolean isNewReservation = (existingReservation == null);
        
        // Si es nueva, validar que el libro existe
        if (isNewReservation) {
            Book book = bookManagerService.getBookById(bookId);
            if (book == null) {
                throw new Exception("reservation.bookNotFound");
            }
        }
        
        // Verificar y reducir stock (común para ambos casos)
        if (!bookManagerService.checkStockAvailability(bookId, quantity)) {
            throw new Exception("reservation.notEnoughStock");
        }
        
        boolean stockReduced = bookManagerService.reduceStock(bookId, quantity);
        if (!stockReduced) {
            throw new Exception("reservation.stockReductionFailed");
        }
        
        try {
            Reservation reservation;
            
            if (isNewReservation) {
                // Crear nueva reserva
                logger.debug("Creating new reservation");
                reservation = reservationDataService.createReservation(username, bookId, quantity);
                updateBookPrice(reservation);
                logger.debug("Reservation created successfully with ID: {}", reservation.getId());
            } else {
                // Actualizar reserva existente
                logger.debug("Existing reservation found, incrementing quantity");
                int newQuantity = existingReservation.getQuantity() + quantity;
                existingReservation.setQuantity(newQuantity);
                reservation = reservationDataService.updateReservation(existingReservation);
                logger.debug("Reservation quantity updated successfully. New quantity: {}", newQuantity);
            }
            
            return reservation;
            
        } catch (Exception e) {
            // Rollback: restaurar stock si falla
            logger.error("Failed to {} reservation, restoring stock", isNewReservation ? "create" : "update", e);
            bookManagerService.increaseStock(bookId, quantity);
            throw e;
        }
    }
}
