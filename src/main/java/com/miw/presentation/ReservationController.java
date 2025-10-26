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
import com.miw.model.Cart;
import com.miw.model.CartItem;
import com.miw.model.Book;
import com.miw.model.Reservation;

@Controller
public class ReservationController {
    
    @Autowired
    private ReservationManagerService reservationManagerService;
    
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
                    
                    // Actualizar cantidad en el carrito
                    Cart cart = (Cart) session.getAttribute("cart");
                    if (cart != null) {
                        boolean found = false;
                        for (CartItem item : cart.getItems()) {
                            if (item.getBookId() == bookId && item.isReserved()) {
                                item.setQuantity(updated.getQuantity()); // Usar cantidad actualizada de BD
                                found = true;
                                break;
                            }
                        }
                        
                        // Si no se encontró en el carrito, añadirlo
                        if (!found) {
                            CartItem item = new CartItem(updated.getBook(), updated.getQuantity(), true);
                            cart.getItems().add(item);
                        }
                        
                        session.setAttribute("cart", cart);
                    }
                    
                    model.addAttribute("message", "reservation.updated");
                    return "redirect:viewCart";
                }
                
                // No existe reserva previa, crear una nueva
                // 1. Crear reserva en BD (reduce stock)
                Reservation reservation = reservationManagerService.createReservation(username, bookId, quantity);
                
                // 2. Obtener o crear carrito en sesión
                Cart cart = (Cart) session.getAttribute("cart");
                if (cart == null) {
                    cart = new Cart();
                }
                
                // 3. Añadir al carrito con marca de reserva
                Book book = reservation.getBook();
                CartItem item = new CartItem(book, quantity, true); // isReserved = true
                cart.getItems().add(item);
                
                // 4. Actualizar carrito en sesión
                session.setAttribute("cart", cart);
                
                session.setAttribute("message", "reservation.created");
                return "redirect:showBooks";
                
            } catch (Exception e) {
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
                
                // 1. Obtener información de la reserva ANTES de eliminarla
                Reservation res = reservationManagerService.getReservations(username).stream()
                    .filter(r -> r.getId() == reservationId)
                    .findFirst()
                    .orElse(null);
                
                if (res == null) {
                    model.addAttribute("error", "reservation.notFound");
                    return "redirect:myReservations";
                }
                
                int bookId = res.getBook().getId();
                
                // 2. Comprar la reserva (eliminar de BD, stock ya reducido)
                reservationManagerService.purchaseReservation(reservationId);
                
                // 3. Quitar del carrito de sesión
                Cart cart = (Cart) session.getAttribute("cart");
                if (cart != null) {
                    cart.removeItem(bookId);
                    session.setAttribute("cart", cart);
                }
                
                model.addAttribute("message", "reservation.purchased");
                return "redirect:myReservations";
                
            } catch (Exception e) {
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
                
                // 1. Obtener info de la reserva antes de cancelar
                Reservation res = reservationManagerService.getReservations(username).stream()
                    .filter(r -> r.getId() == reservationId)
                    .findFirst()
                    .orElse(null);
                
                // 2. Cancelar reserva (restaura stock)
                reservationManagerService.cancelReservation(reservationId);
                
                // 3. Quitar del carrito de sesión
                Cart cart = (Cart) session.getAttribute("cart");
                if (cart != null && res != null) {
                    cart.removeItem(res.getBook().getId());
                    session.setAttribute("cart", cart);
                }
                
                model.addAttribute("message", "reservation.cancelled");
                return "redirect:myReservations";
                
            } catch (Exception e) {
                model.addAttribute("error", "error.general");
                return "redirect:myReservations";
            }
        }
    }
}
