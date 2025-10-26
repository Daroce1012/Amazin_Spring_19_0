package com.miw.presentation;

import java.security.Principal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import com.miw.business.reservationmanager.ReservationManagerService;
import com.miw.business.cartmanager.CartSessionService;
import com.miw.model.Cart;
import com.miw.model.Reservation;
import org.apache.logging.log4j.*;

@Controller
public class ReservationController {
    
    Logger logger = LogManager.getLogger(this.getClass());
    
    @Autowired
    private ReservationManagerService reservationManagerService;
    
    @Autowired
    private CartSessionService cartSessionService;
    
    @Autowired
    private ServletContext servletContext;
    
    @RequestMapping("private/reserveBook")
    public String reserveBook(
            @RequestParam("bookId") int bookId,
            @RequestParam("quantity") int quantity,
            Principal principal,
            HttpSession session,
            Model model) {
        
        // Sincronización para evitar condiciones de carrera
        synchronized (servletContext) {
            try {
                String username = principal.getName();
                
                // Verificar si ya existe una reserva de este libro
                Reservation existingReservation = reservationManagerService.getReservationByUserAndBook(username, bookId);
                
                if (existingReservation != null) {
                    // Ya existe una reserva, incrementar cantidad usando el manager
                    Reservation updated = reservationManagerService.incrementReservationQuantity(existingReservation.getId(), quantity);
                    
                    // Actualizar cantidad en el carrito usando método helper del servicio
                    cartSessionService.updateReservedItemInCart(session, updated.getBook(), updated.getQuantity());
                    
                    model.addAttribute("message", "reservation.updated");
                    return "redirect:viewCart";
                }
                
                // No existe reserva previa, crear una nueva
                // 1. Crear reserva en BD (reduce stock)
                Reservation reservation = reservationManagerService.createReservation(username, bookId, quantity);
                
                // 2. Añadir al carrito usando método helper del servicio
                cartSessionService.addReservedItemToCart(session, reservation.getBook(), quantity);
                
                session.setAttribute("message", "reservation.created");
                return "redirect:showBooks";
                
            } catch (Exception e) {
                logger.error("Error creating reservation for user " + principal.getName(), e);
                session.setAttribute("error", "error.general");
                return "redirect:showBooks";
            }
        }
    }
    
    @RequestMapping("private/myReservations")
    public String myReservations(Principal principal, Model model) {
        
        try {
            String username = principal.getName();
            
            // Obtener reservas del usuario
            List<Reservation> reservations = reservationManagerService.getReservations(username);
            
            model.addAttribute("reservations", reservations);
            return "private/myReservations";
            
            } catch (Exception e) {
                logger.error("Error getting reservations for user " + principal.getName(), e);
                model.addAttribute("error", "error.general");
                return "private/error";
            }
    }
    
    @RequestMapping("private/purchaseReservation")
    public String purchaseReservation(
            @RequestParam("reservationId") int reservationId,
            Principal principal,
            HttpSession session,
            Model model) {
        
        synchronized (servletContext) {
            try {
                String username = principal.getName();
                
                // Usar método helper para procesar reserva y actualizar carrito
                processReservationAndRemoveFromCart(reservationId, username, session, true);
                
                model.addAttribute("message", "reservation.purchased");
                return "redirect:myReservations";
                
            } catch (Exception e) {
                logger.error("Error purchasing reservation " + reservationId, e);
                model.addAttribute("error", "error.general");
                return "redirect:myReservations";
            }
        }
    }
    
    @RequestMapping("private/cancelReservationFromPage")
    public String cancelReservationFromPage(
            @RequestParam("reservationId") int reservationId,
            Principal principal,
            HttpSession session,
            Model model) {
        
        synchronized (servletContext) {
            try {
                String username = principal.getName();
                
                // Usar método helper para procesar reserva y actualizar carrito
                processReservationAndRemoveFromCart(reservationId, username, session, false);
                
                model.addAttribute("message", "reservation.cancelled");
                return "redirect:myReservations";
                
            } catch (Exception e) {
                logger.error("Error cancelling reservation " + reservationId, e);
                model.addAttribute("error", "error.general");
                return "redirect:myReservations";
            }
        }
    }
    
    /**
     * Método helper privado para procesar una reserva y actualizar el carrito.
     * Elimina duplicación entre purchaseReservation y cancelReservationFromPage.
     * 
     * @param reservationId ID de la reserva a procesar
     * @param username Usuario propietario de la reserva
     * @param session Sesión HTTP
     * @param isPurchase true para comprar, false para cancelar
     */
    private void processReservationAndRemoveFromCart(
            int reservationId, 
            String username, 
            HttpSession session,
            boolean isPurchase) throws Exception {
        
        // 1. Obtener información de la reserva
        Reservation res = reservationManagerService.getReservationById(reservationId, username);
        
        // 2. Procesar la reserva (comprar o cancelar)
        if (isPurchase) {
            reservationManagerService.purchaseReservation(reservationId);
        } else {
            reservationManagerService.cancelReservation(reservationId);
        }
        
        // 3. Quitar del carrito usando método helper del servicio
        cartSessionService.removeItemFromCart(session, res.getBook().getId());
    }
}
