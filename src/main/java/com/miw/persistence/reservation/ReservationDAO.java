package com.miw.persistence.reservation;

import java.util.List;
import org.apache.logging.log4j.*;
import com.miw.model.Book;
import com.miw.model.Reservation;
import com.miw.persistence.Dba;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

public class ReservationDAO implements ReservationDataService {
    
    Logger logger = LogManager.getLogger(this.getClass());
    
    @Override
    public Reservation createReservation(String username, int bookId, int quantity) throws Exception {
        logger.debug("Creating reservation for book: " + bookId + ", user: " + username);
        
        Dba dba = new Dba();
        try {
            EntityManager em = dba.getActiveEm();
            
            // Obtener el Book en el MISMO contexto de persistencia
            Book book = em.find(Book.class, bookId);
            if (book == null) {
                throw new Exception("Book not found: " + bookId);
            }
            
            // Crear la reserva con el Book obtenido en este contexto
            Reservation reservation = new Reservation();
            reservation.setBook(book);
            reservation.setUsername(username);
            reservation.setQuantity(quantity);
            
            em.persist(reservation);
            logger.debug("Reservation created successfully with ID: " + reservation.getId());
            return reservation;
        } catch (Exception e) {
            logger.error("Error creating reservation", e);
            throw new Exception("Error creating reservation", e);
        } finally {
            dba.closeEm();
        }
    }
    
    @Override
    public List<Reservation> getReservationsByUsername(String username) throws Exception {
        logger.debug("Getting reservations for user: " + username);
        
        Dba dba = new Dba(true); // Solo lectura
        try {
            EntityManager em = dba.getActiveEm();
            
            TypedQuery<Reservation> query = em.createQuery(
                "SELECT r FROM Reservation r WHERE r.username = :username ORDER BY r.id DESC", 
                Reservation.class
            );
            query.setParameter("username", username);
            
            List<Reservation> results = query.getResultList();
            
            // Forzar carga de libros para evitar lazy loading issues
            for (Reservation r : results) {
                r.getBook().getTitle();
            }
            
            return results;
        } catch (Exception e) {
            logger.error("Error getting reservations for user: " + username, e);
            throw new Exception("Error getting reservations", e);
        } finally {
            dba.closeEm();
        }
    }
    
    @Override
    public Reservation getReservationById(int id) throws Exception {
        logger.debug("Getting reservation by ID: " + id);
        
        Dba dba = new Dba(true); // Solo lectura
        try {
            EntityManager em = dba.getActiveEm();
            Reservation reservation = em.find(Reservation.class, id);
            
            if (reservation != null) {
                // Forzar carga del libro para evitar lazy loading issues
                reservation.getBook().getTitle();
            }
            
            return reservation;
        } catch (Exception e) {
            logger.error("Error getting reservation by ID: " + id, e);
            throw new Exception("Error getting reservation", e);
        } finally {
            dba.closeEm();
        }
    }
    
    @Override
    public void deleteReservation(int id) throws Exception {
        logger.debug("Deleting reservation: " + id);
        
        Dba dba = new Dba();
        try {
            EntityManager em = dba.getActiveEm();
            Reservation reservation = em.find(Reservation.class, id);
            if (reservation != null) {
                em.remove(reservation);
                logger.debug("Reservation deleted successfully");
            } else {
                logger.warn("Reservation not found: " + id);
            }
        } catch (Exception e) {
            logger.error("Error deleting reservation", e);
            throw new Exception("Error deleting reservation", e);
        } finally {
            dba.closeEm();
        }
    }
    
    @Override
    public Reservation updateReservation(Reservation reservation) throws Exception {
        logger.debug("Updating reservation: " + reservation.getId());
        Dba dba = new Dba();
        try {
            EntityManager em = dba.getActiveEm();
            
            // Obtener la reserva gestionada por este EntityManager
            Reservation managed = em.find(Reservation.class, reservation.getId());
            if (managed == null) {
                throw new Exception("Reservation not found: " + reservation.getId());
            }
            
            // Actualizar solo el campo quantity (no tocar el Book ni sus cascadas)
            managed.setQuantity(reservation.getQuantity());
            
            // Forzar carga del libro para evitar lazy loading issues
            managed.getBook().getTitle();
            
            // El flush automático hará el UPDATE al cerrar la transacción
            logger.debug("Reservation updated successfully");
            return managed;
        } catch (Exception e) {
            logger.error("Error updating reservation", e);
            throw new Exception("Error updating reservation", e);
        } finally {
            dba.closeEm();
        }
    }
}