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
            
            // Usar clave de internacionalización y volver al catálogo
            session.setAttribute("message", "cart.bookAddedSuccessfully");
            return "redirect:showBooks";
            
        } catch (Exception e) {
            // Mostrar error y volver al catálogo
            session.setAttribute("error", "error.general");
            return "redirect:showBooks";
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
            model.addAttribute("error", "error.general");
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
                
                // Buscar el item usando método encapsulado
                CartItem itemToRemove = cart.findItemByBookId(bookId);
                
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
                model.addAttribute("error", "error.general");
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
                    
                    // Cancelar todas las reservas usando método del servicio
                    reservationManagerService.cancelAllReservationsInCart(username, cart);
                    
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
                model.addAttribute("error", "error.general");
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
                
                // Buscar el item en el carrito usando método encapsulado
                CartItem itemToPurchase = cart.findItemByBookId(bookId);
                
                if (itemToPurchase == null) {
                    session.setAttribute("error", "cart.itemNotFound");
                    return "redirect:viewCart";
                }
                
                // Procesar la compra usando servicio de negocio (maneja reservas y compras normales)
                cartManagerService.purchaseCartItem(
                    username, 
                    itemToPurchase.getBookId(), 
                    itemToPurchase.getQuantity(),
                    itemToPurchase.isReserved()
                );
                
                // Quitar el item del carrito usando servicio
                cartSessionService.removeItemFromCart(cart, bookId, itemToPurchase.isReserved());
                
                // Actualizar carrito en sesión
                cartSessionService.updateCart(session, cart);
                
                session.setAttribute("message", "cart.itemPurchased");
                return "redirect:viewCart";
                
            } catch (Exception e) {
                logger.error("Error purchasing item", e);
                
                // Manejar errores específicos
                String errorMsg = e.getMessage();
                if ("reservation.notFound".equals(errorMsg)) {
                    session.setAttribute("error", "reservation.notFound");
                } else if ("cart.notEnoughStock".equals(errorMsg)) {
                    session.setAttribute("error", "cart.notEnoughStock");
                } else {
                    session.setAttribute("error", "cart.checkoutError");
                }
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
