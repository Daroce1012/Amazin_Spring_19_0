package com.miw.presentation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import com.miw.business.cartmanager.CartManagerService;
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
    private ServletContext servletContext;
    
    public void setCartManagerService(CartManagerService cartManagerService) {
        this.cartManagerService = cartManagerService;
    }
    
    @RequestMapping("private/addToCart")
    public String addToCart(
            @RequestParam("bookId") int bookId,
            @RequestParam("quantity") int quantity,
            HttpSession session,
            Model model) {
        
        try {
            // Obtener o crear carrito en sesión
            Cart cart = (Cart) session.getAttribute("cart");
            if (cart == null) {
                cart = new Cart();
            }
            
            // Añadir libro al carrito
            cartManagerService.addBookToCart(cart, bookId, quantity);
            
            // Actualizar carrito en sesión
            session.setAttribute("cart", cart);
            
            // Usar clave de internacionalización
            model.addAttribute("message", "cart.bookAddedSuccessfully");
            return "redirect:viewCart";
            
        } catch (Exception e) {
            // Asegurar que el carrito esté disponible en el modelo para mostrar el error
            Cart cart = (Cart) session.getAttribute("cart");
            if (cart == null) {
                cart = new Cart();
                session.setAttribute("cart", cart);
            }
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
            Cart cart = (Cart) session.getAttribute("cart");
            if (cart == null) {
                cart = new Cart();
                session.setAttribute("cart", cart);
            }
            
            // Sincronizar con reservas de BD
            List<Reservation> reservations = reservationManagerService.getReservations(username);
            
            // Primero, eliminar del carrito reservas que ya no existen en BD
            cart.getItems().removeIf(item -> {
                if (item.isReserved()) {
                    boolean existsInDB = reservations.stream()
                        .anyMatch(r -> r.getBook().getId() == item.getBookId());
                    return !existsInDB; // Eliminar si no existe en BD
                }
                return false;
            });
            
            // Luego, agregar o actualizar reservas desde BD
            for (Reservation res : reservations) {
                CartItem existingItem = cart.getItems().stream()
                    .filter(item -> item.getBookId() == res.getBook().getId() && item.isReserved())
                    .findFirst()
                    .orElse(null);
                
                if (existingItem != null) {
                    // Actualizar cantidad desde BD (BD es la fuente de verdad)
                    existingItem.setQuantity(res.getQuantity());
                } else {
                    // Añadir nueva reserva al carrito
                    CartItem item = new CartItem(res.getBook(), res.getQuantity(), true);
                    cart.getItems().add(item);
                }
            }
            
            // Actualizar carrito en sesión después de sincronizar
            session.setAttribute("cart", cart);
            
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
                Cart cart = (Cart) session.getAttribute("cart");
                if (cart == null) {
                    return "redirect:viewCart";
                }
                
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
                    
                    // Quitar del carrito solo el item específico (reserva o no)
                    if (itemToRemove.isReserved()) {
                        cart.removeReservedItem(bookId);
                    } else {
                        cart.removeNonReservedItem(bookId);
                    }
                    
                    // Actualizar carrito en sesión
                    session.setAttribute("cart", cart);
                    
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
                Cart cart = (Cart) session.getAttribute("cart");
                
                if (cart != null && !cart.isEmpty()) {
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
                    session.setAttribute("cart", cart);
                    
                    model.addAttribute("message", "cart.cartCleared");
                } else {
                    cart = new Cart();
                    session.setAttribute("cart", cart);
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
                Cart cart = (Cart) session.getAttribute("cart");
                
                if (cart == null) {
                    model.addAttribute("error", "cart.empty");
                    return "redirect:viewCart";
                }
                
                // Buscar el item en el carrito
                CartItem itemToPurchase = cart.getItems().stream()
                    .filter(item -> item.getBookId() == bookId)
                    .findFirst()
                    .orElse(null);
                
                if (itemToPurchase == null) {
                    model.addAttribute("error", "cart.itemNotFound");
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
                        model.addAttribute("error", "reservation.notFound");
                        return "redirect:viewCart";
                    }
                } else {
                    // Es compra normal: reducir stock
                    boolean reduced = cartManagerService.reduceStockForPurchase(
                        itemToPurchase.getBookId(), 
                        itemToPurchase.getQuantity()
                    );
                    
                    if (!reduced) {
                        model.addAttribute("error", "cart.notEnoughStock");
                        return "redirect:viewCart";
                    }
                    logger.debug("Stock reduced for normal purchase");
                }
                
                // Quitar el item del carrito
                if (itemToPurchase.isReserved()) {
                    cart.removeReservedItem(bookId);
                } else {
                    cart.removeNonReservedItem(bookId);
                }
                
                // Actualizar carrito en sesión
                session.setAttribute("cart", cart);
                
                model.addAttribute("message", "cart.itemPurchased");
                return "redirect:viewCart";
                
            } catch (Exception e) {
                logger.error("Error purchasing item", e);
                model.addAttribute("error", "cart.checkoutError");
                return "redirect:viewCart";
            }
        }
    }
    
    @RequestMapping("private/checkout")
    public String checkout(Principal principal, HttpSession session, Model model) {
        
        Cart cart = (Cart) session.getAttribute("cart");
        
        if (cart == null || cart.getItems().isEmpty()) {
            model.addAttribute("error", "cart.empty");
            return "private/viewCart";
        }
        
        // SINCRONIZACIÓN USANDO EL CONTEXTO DE LA APLICACIÓN
        // Esto previene que dos usuarios compren el mismo stock simultáneamente
        synchronized (servletContext) {
            try {
                String username = principal.getName();
                boolean allSuccess = true;
                
                for (CartItem item : cart.getItems()) {
                    if (item.isReserved()) {
                        // Es reserva: eliminar de BD (stock YA está reducido)
                        Reservation res = reservationManagerService.getReservationByUserAndBook(username, item.getBookId());
                        
                        if (res != null) {
                            reservationManagerService.purchaseReservation(res.getId());
                        }
                    } else {
                        // Es compra normal: reducir stock
                        boolean reduced = cartManagerService.reduceStockForPurchase(
                            item.getBookId(), 
                            item.getQuantity()
                        );
                        
                        if (!reduced) {
                            allSuccess = false;
                            break;
                        }
                    }
                }
                
                if (allSuccess) {
                    model.addAttribute("message", "cart.purchaseSuccess");
                    session.removeAttribute("cart");
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
