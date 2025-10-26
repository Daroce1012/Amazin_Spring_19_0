package com.miw.presentation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import com.miw.business.cartmanager.CartManagerService;
import com.miw.business.cartmanager.CartSessionService;
import com.miw.business.reservationmanager.ReservationManagerService;
import com.miw.model.Cart;
import com.miw.model.CartItem;
import com.miw.model.Reservation;
import java.security.Principal;
import java.util.List;
import org.apache.logging.log4j.*;

@Controller
public class CartController {
    
    Logger logger = LogManager.getLogger(this.getClass());
    
    @Autowired
    private CartManagerService cartManagerService;
    
    @Autowired
    private ReservationManagerService reservationManagerService;
    
    @Autowired
    private CartSessionService cartSessionService;
    
    @Autowired
    private ServletContext servletContext;
    
    @RequestMapping("private/addToCart")
    public String addToCart(
            @RequestParam("bookId") int bookId,
            @RequestParam("quantity") int quantity,
            HttpSession session,
            Model model) {
        
        try {
            // Obtener o crear carrito en sesión
            Cart cart = cartSessionService.getOrCreateCart(session);
            
            // Añadir libro al carrito
            cartManagerService.addBookToCart(cart, bookId, quantity);
            
            // Actualizar carrito en sesión
            cartSessionService.updateCart(session, cart);
            
            // Usar clave de internacionalización
            model.addAttribute("message", "cart.bookAddedSuccessfully");
            return "redirect:viewCart";
            
        } catch (Exception e) {
            // Asegurar que el carrito esté disponible en el modelo para mostrar el error
            Cart cart = cartSessionService.getOrCreateCart(session);
            model.addAttribute("cart", cart);
            model.addAttribute("total", cart.getTotal());
            model.addAttribute("error", e.getMessage());
            return "private/viewCart";
        }
    }
    
    @RequestMapping("private/viewCart")
    public String viewCart(Principal principal, HttpSession session, Model model) {
        
        try {
            String username = principal.getName();
            
            // Obtener carrito de sesión
            Cart cart = cartSessionService.getOrCreateCart(session);
            
            // Obtener reservas de BD
            List<Reservation> reservations = reservationManagerService.getReservations(username);
            
            // Sincronizar usando servicio de negocio
            cartManagerService.synchronizeCartWithReservations(cart, reservations);
            
            // Actualizar carrito en sesión después de sincronizar
            cartSessionService.updateCart(session, cart);
            
            model.addAttribute("cart", cart);
            model.addAttribute("total", cart.getTotal());
            
            return "private/viewCart";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "private/error";
        }
    }
    
    @RequestMapping("private/removeFromCart")
    public String removeFromCart(
            @RequestParam("bookId") int bookId,
            Principal principal,
            HttpSession session,
            Model model) {
        
        synchronized (servletContext) {
            try {
                Cart cart = cartSessionService.getOrCreateCart(session);
                
                // Buscar el item
                CartItem itemToRemove = cart.getItems().stream()
                    .filter(item -> item.getBookId() == bookId)
                    .findFirst()
                    .orElse(null);
                
                if (itemToRemove != null) {
                    // Si es reserva, cancelarla en BD (restaura stock)
                    if (itemToRemove.isReserved()) {
                        String username = principal.getName();
                        boolean cancelled = reservationManagerService.cancelReservationByUserAndBook(username, bookId);
                        
                        if (!cancelled) {
                            logger.warn("No se pudo cancelar la reserva para bookId: " + bookId);
                        }
                    }
                    
                    // Quitar del carrito usando servicio
                    cartSessionService.removeItemFromCart(cart, bookId, itemToRemove.isReserved());
                    
                    // Actualizar carrito en sesión
                    cartSessionService.updateCart(session, cart);
                    
                    model.addAttribute("message", "cart.itemRemoved");
                }
                
                return "redirect:viewCart";
                
            } catch (Exception e) {
                model.addAttribute("error", e.getMessage());
                return "redirect:viewCart";
            }
        }
    }
    
    @RequestMapping("private/clearCart")
    public String clearCart(Principal principal, HttpSession session, Model model) {
        
        synchronized (servletContext) {
            try {
                Cart cart = cartSessionService.getOrCreateCart(session);
                
                if (!cart.isEmpty()) {
                    String username = principal.getName();
                    
                    // Cancelar reservas antes de vaciar
                    for (CartItem item : cart.getItems()) {
                        if (item.isReserved()) {
                            reservationManagerService.cancelReservationByUserAndBook(username, item.getBookId());
                        }
                    }
                    
                    // Vaciar el carrito
                    cart.clear();
                    
                    // Actualizar carrito en sesión
                    cartSessionService.updateCart(session, cart);
                    
                    model.addAttribute("message", "cart.cartCleared");
                }
                
                model.addAttribute("cart", cart);
                model.addAttribute("total", cart.getTotal());
                
                return "private/viewCart";
                
            } catch (Exception e) {
                model.addAttribute("error", e.getMessage());
                return "private/viewCart";
            }
        }
    }
    
    @RequestMapping("private/purchaseItem")
    public String purchaseItem(
            @RequestParam("bookId") int bookId,
            Principal principal,
            HttpSession session,
            Model model) {
        
        synchronized (servletContext) {
            try {
                String username = principal.getName();
                Cart cart = cartSessionService.getOrCreateCart(session);
                
                // Buscar el item en el carrito
                CartItem itemToPurchase = cart.getItems().stream()
                    .filter(item -> item.getBookId() == bookId)
                    .findFirst()
                    .orElse(null);
                
                if (itemToPurchase == null) {
                    session.setAttribute("error", "cart.itemNotFound");
                    return "redirect:viewCart";
                }
                
                // Procesar la compra según el tipo
                if (itemToPurchase.isReserved()) {
                    // Es reserva: completar pago del 95% restante
                    Reservation res = reservationManagerService.getReservationByUserAndBook(username, bookId);
                    
                    if (res != null) {
                        reservationManagerService.purchaseReservation(res.getId());
                        logger.debug("Reservation purchased: " + res.getId());
                    } else {
                        session.setAttribute("error", "reservation.notFound");
                        return "redirect:viewCart";
                    }
                } else {
                    // Es compra normal: reducir stock
                    boolean reduced = cartManagerService.reduceStockForPurchase(
                        itemToPurchase.getBookId(), 
                        itemToPurchase.getQuantity()
                    );
                    
                    if (!reduced) {
                        session.setAttribute("error", "cart.notEnoughStock");
                        return "redirect:viewCart";
                    }
                    logger.debug("Stock reduced for normal purchase");
                }
                
                // Quitar el item del carrito usando servicio
                cartSessionService.removeItemFromCart(cart, bookId, itemToPurchase.isReserved());
                
                // Actualizar carrito en sesión
                cartSessionService.updateCart(session, cart);
                
                session.setAttribute("message", "cart.itemPurchased");
                return "redirect:viewCart";
                
            } catch (Exception e) {
                logger.error("Error purchasing item", e);
                session.setAttribute("error", "cart.checkoutError");
                return "redirect:viewCart";
            }
        }
    }
    
    @RequestMapping("private/checkout")
    public String checkout(Principal principal, HttpSession session, Model model) {
        
        Cart cart = cartSessionService.getOrCreateCart(session);
        
        if (cart.isEmpty()) {
            model.addAttribute("error", "cart.empty");
            return "private/viewCart";
        }
        
        // SINCRONIZACIÓN USANDO EL CONTEXTO DE LA APLICACIÓN
        // Esto previene que dos usuarios compren el mismo stock simultáneamente
        synchronized (servletContext) {
            try {
                String username = principal.getName();
                
                // Procesar reservas usando servicio de negocio
                reservationManagerService.processReservationsInCart(username, cart);
                
                // Procesar compras normales usando servicio de negocio
                boolean success = cartManagerService.processNormalPurchases(cart);
                
                if (success) {
                    model.addAttribute("message", "cart.purchaseSuccess");
                    cartSessionService.clearCart(session);
                    return "private/checkoutSuccess";
                } else {
                    model.addAttribute("error", "cart.someItemsOutOfStock");
                    return "private/viewCart";
                }
                
            } catch (Exception e) {
                model.addAttribute("error", "cart.checkoutError");
                return "private/error";
            }
        }
    }
}
