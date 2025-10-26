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
    
    Logger logger = LogManager.getLogger(this.getClass());
    
    @Autowired
    private ReservationDataService reservationDataService;
    
    @Autowired
    private BookManagerService bookManagerService;
    
    @Override
    public Reservation createReservation(String username, int bookId, int quantity) throws Exception {
        logger.debug("Creating reservation for user " + username + ", book " + bookId + ", qty: " + quantity);
        
        // 1. Verificar que el libro existe (usando BookManager para el precio)
        Book book = bookManagerService.getBookById(bookId);
        if (book == null) {
            throw new Exception("reservation.bookNotFound");
        }
        
        // 2. Verificar stock disponible
        if (!bookManagerService.checkStockAvailability(bookId, quantity)) {
            throw new Exception("reservation.notEnoughStock");
        }
        
        // 3. Reducir el stock (la reserva bloquea unidades)
        boolean stockReduced = bookManagerService.reduceStock(bookId, quantity);
        if (!stockReduced) {
            throw new Exception("reservation.stockReductionFailed");
        }
        
        // 4. Crear la reserva en la BD - AHORA SOLO PASA EL ID
        try {
            Reservation reservation = reservationDataService.createReservation(username, bookId, quantity);
            
            // 5. Actualizar el precio del libro en la reserva (para visualización)
            reservation.getBook().setPrice(book.getPrice());
            
            logger.debug("Reservation created successfully with ID: " + reservation.getId());
            return reservation;
        } catch (Exception e) {
            // Si falla la creación de la reserva, restaurar el stock
            logger.error("Failed to create reservation, restoring stock", e);
            bookManagerService.increaseStock(bookId, quantity);
            throw e;
        }
    }
    
    @Override
    public List<Reservation> getReservations(String username) throws Exception {
        logger.debug("Getting reservations for user: " + username);
        List<Reservation> reservations = reservationDataService.getReservationsByUsername(username);
        
        // Calcular precios de los libros (como hace BookManager)
        for (Reservation reservation : reservations) {
            Book book = reservation.getBook();
            if (book != null) {
                // Recalcular precio con IVA usando BookManagerService
                Book bookWithPrice = bookManagerService.getBookById(book.getId());
                if (bookWithPrice != null) {
                    book.setPrice(bookWithPrice.getPrice());
                }
            }
        }
        
        return reservations;
    }
    
    @Override
    public Reservation getReservationByUserAndBook(String username, int bookId) throws Exception {
        logger.debug("Getting reservation for user: " + username + ", book: " + bookId);
        // Usar getReservations que ya calcula los precios
        List<Reservation> reservations = getReservations(username);
        return reservations.stream()
            .filter(r -> r.getBook().getId() == bookId)
            .findFirst()
            .orElse(null);
    }
    
    @Override
    public Reservation getReservationById(int reservationId, String username) throws Exception {
        logger.debug("Getting reservation by ID: " + reservationId + " for user: " + username);
        
        // Obtener la reserva directamente por ID
        Reservation reservation = reservationDataService.getReservationById(reservationId);
        
        if (reservation == null) {
            throw new Exception("reservation.notFound");
        }
        
        // Validar que la reserva pertenece al usuario (seguridad)
        if (!reservation.getUsername().equals(username)) {
            logger.warn("User " + username + " tried to access reservation " + reservationId + " belonging to " + reservation.getUsername());
            throw new Exception("reservation.accessDenied");
        }
        
        // Actualizar precio del libro (como en otros métodos)
        Book book = reservation.getBook();
        if (book != null) {
            Book bookWithPrice = bookManagerService.getBookById(book.getId());
            if (bookWithPrice != null) {
                book.setPrice(bookWithPrice.getPrice());
            }
        }
        
        return reservation;
    }
    
    @Override
    public boolean purchaseReservation(int reservationId) throws Exception {
        logger.debug("Purchasing reservation: " + reservationId);
        
        // 1. Obtener la reserva
        Reservation reservation = reservationDataService.getReservationById(reservationId);
        
        if (reservation == null) {
            throw new Exception("reservation.notFound");
        }
        
        // 2. Eliminar la reserva (ya se pagó el 5%, ahora se paga el resto)
        //    El stock ya está reducido desde que se creó la reserva
        reservationDataService.deleteReservation(reservationId);
        
        logger.debug("Reservation purchased and deleted. Stock remains reduced.");
        return true;
    }
    
    @Override
    public boolean cancelReservation(int reservationId) throws Exception {
        logger.debug("Cancelling reservation: " + reservationId);
        
        // 1. Obtener la reserva
        Reservation reservation = reservationDataService.getReservationById(reservationId);
        
        if (reservation == null) {
            throw new Exception("reservation.notFound");
        }
        
        // 2. Restaurar el stock antes de eliminar
        Book book = reservation.getBook();
        bookManagerService.increaseStock(book.getId(), reservation.getQuantity());
        logger.debug("Stock restored for book " + book.getId());
        
        // 3. Eliminar la reserva
        reservationDataService.deleteReservation(reservationId);
        
        logger.debug("Reservation cancelled and deleted");
        return true;
    }
    
    @Override
    public boolean cancelReservationByUserAndBook(String username, int bookId) throws Exception {
        logger.debug("Cancelling reservation for user: " + username + ", book: " + bookId);
        
        // 1. Buscar la reserva
        Reservation reservation = getReservationByUserAndBook(username, bookId);
        
        if (reservation == null) {
            logger.warn("No reservation found for user: " + username + ", book: " + bookId);
            return false;
        }
        
        // 2. Cancelar usando el método existente
        return cancelReservation(reservation.getId());
    }
    
    @Override
    public Reservation incrementReservationQuantity(int reservationId, int additionalQuantity) throws Exception {
        logger.debug("Incrementing reservation quantity: " + reservationId + " by " + additionalQuantity);
        
        // 1. Obtener la reserva
        Reservation reservation = reservationDataService.getReservationById(reservationId);
        
        if (reservation == null) {
            throw new Exception("reservation.notFound");
        }
        
        int bookId = reservation.getBook().getId();
        
        // 2. Verificar stock disponible para la cantidad adicional
        if (!bookManagerService.checkStockAvailability(bookId, additionalQuantity)) {
            throw new Exception("reservation.notEnoughStock");
        }
        
        // 3. Reducir el stock por la cantidad adicional
        boolean stockReduced = bookManagerService.reduceStock(bookId, additionalQuantity);
        if (!stockReduced) {
            throw new Exception("reservation.stockReductionFailed");
        }
        
        // 4. Actualizar la cantidad en la reserva
        int newQuantity = reservation.getQuantity() + additionalQuantity;
        reservation.setQuantity(newQuantity);
        
        // 5. Guardar en BD
        reservation = reservationDataService.updateReservation(reservation);
        
        logger.debug("Reservation quantity updated successfully. New quantity: " + newQuantity);
        return reservation;
    }
    
    @Override
    public boolean processReservationsInCart(String username, Cart cart) throws Exception {
        logger.debug("Processing reservations in cart for user: " + username);
        
        for (CartItem item : cart.getItems()) {
            if (item.isReserved()) {
                Reservation res = getReservationByUserAndBook(username, item.getBookId());
                if (res != null) {
                    purchaseReservation(res.getId());
                }
            }
        }
        
        return true;
    }
    
    @Override
    public void cancelAllReservationsInCart(String username, Cart cart) throws Exception {
        logger.debug("Cancelling all reservations in cart for user: " + username);
        
        for (CartItem item : cart.getItems()) {
            if (item.isReserved()) {
                boolean cancelled = cancelReservationByUserAndBook(username, item.getBookId());
                if (!cancelled) {
                    logger.warn("Could not cancel reservation for bookId: " + item.getBookId());
                }
            }
        }
    }
}
