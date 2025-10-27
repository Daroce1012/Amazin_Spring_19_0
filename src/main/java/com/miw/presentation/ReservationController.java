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
import com.miw.model.Reservation;
import org.apache.logging.log4j.*;

@Controller
public class ReservationController {
    
    private static final Logger logger = LogManager.getLogger(ReservationController.class);
    
    @Autowired
    private ReservationManagerService reservationManagerService;
    
    @Autowired
    private ServletContext servletContext;
    
    @RequestMapping("private/reserveBook")
    public String reserveBook(@RequestParam("bookId") int bookId, @RequestParam("quantity") int quantity,
            Principal principal, HttpSession session) {
        
        synchronized (servletContext) {
            try {
                String username = principal.getName();
                
                // Toda la lógica está encapsulada en el servicio
                reservationManagerService.reserveBook(username, bookId, quantity);
                
                session.setAttribute("message", "reservation.created");
                return "redirect:showBooks";
                
            } catch (Exception e) {
                handleReservationError(e, session, null, "Error reserving book", true);
                return "redirect:showBooks";
            }
        }
    }
    
    @RequestMapping("private/myReservations")
    public String myReservations(Principal principal, Model model) {
        try {
            String username = principal.getName();
            List<Reservation> reservations = reservationManagerService.getReservations(username);
            
            model.addAttribute("reservations", reservations);
            return "private/myReservations";
            
        } catch (Exception e) {
            logger.error("Error getting reservations for user {}", principal.getName(), e);
            model.addAttribute("error", "error.general");
            return "private/error";
        }
    }
    
    @RequestMapping("private/purchaseReservation")
    public String purchaseReservation(@RequestParam("reservationId") int reservationId,
            Principal principal, Model model) {
        
        synchronized (servletContext) {
            try {
                String username = principal.getName();
                
                // El servicio valida internamente la propiedad
                reservationManagerService.purchaseReservation(reservationId, username);
                
                model.addAttribute("message", "reservation.purchased");
                return "redirect:myReservations";
                
            } catch (Exception e) {
                handleReservationError(e, null, model, "Error purchasing reservation", false);
                return "redirect:myReservations";
            }
        }
    }
    
    @RequestMapping("private/cancelReservationFromPage")
    public String cancelReservationFromPage(@RequestParam("reservationId") int reservationId,
            Principal principal, Model model) {
        
        synchronized (servletContext) {
            try {
                String username = principal.getName();
                
                // El servicio valida internamente la propiedad
                reservationManagerService.cancelReservation(reservationId, username);
                
                model.addAttribute("message", "reservation.cancelled");
                return "redirect:myReservations";
                
            } catch (Exception e) {
                handleReservationError(e, null, model, "Error cancelling reservation", false);
                return "redirect:myReservations";
            }
        }
    }
    
    // Método privado auxiliar - Maneja errores estándar de reservas
    private void handleReservationError(Exception e, HttpSession session, Model model, String logMessage, boolean useSession) {
        logger.error(logMessage, e);
        
        String errorMsg = e.getMessage();
        String errorKey = "error.general";
        
        // Si el mensaje es una clave de internacionalización válida, usarla
        if (errorMsg != null && (errorMsg.startsWith("reservation.") || errorMsg.startsWith("cart.") || errorMsg.startsWith("error."))) {
            errorKey = errorMsg;
        }
        
        if (useSession) {
            session.setAttribute("error", errorKey);
        } else {
            model.addAttribute("error", errorKey);
        }
    }
}
