package com.miw.presentation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;
import com.miw.business.cartmanager.CartManagerService;
import com.miw.model.Cart;
import java.security.Principal;
import org.apache.logging.log4j.*;

@Controller
public class CartController {
    
    private static final Logger logger = LogManager.getLogger(CartController.class);
    private static final String CART_ATTRIBUTE = "cart";
    
    @Autowired
    private CartManagerService cartManagerService;
    
  
    
    @RequestMapping("private/addToCart")
    public String addToCart(@RequestParam("bookId") int bookId, @RequestParam("quantity") int quantity,
            HttpSession session, Model model) {
        
        try {
            Cart cart = getOrCreateCart(session);
            cartManagerService.addBookToCart(cart, bookId, quantity);
            session.setAttribute(CART_ATTRIBUTE, cart);
            
            session.setAttribute("message", "cart.bookAddedSuccessfully");
            return "redirect:showBooks";
            
        } catch (Exception e) {
            handleCartError(e, session, model, "Error adding book to cart", true);
            return "redirect:showBooks";
        }
    }
    
    @RequestMapping("private/viewCart")
    public String viewCart(Principal principal, HttpSession session, Model model) {
        
        try {
            String username = principal.getName();
            Cart cart = getOrCreateCart(session);
            
            // Sincronizar con reservas (todo encapsulado en el servicio)
            cartManagerService.synchronizeCartForUser(username, cart);
            session.setAttribute(CART_ATTRIBUTE, cart);
            
            model.addAttribute("total", cart.getTotal());
            return "private/viewCart";
            
        } catch (Exception e) {
            handleCartError(e, session, model, "Error viewing cart", false);
            return "private/error";
        }
    }
    
    @RequestMapping("private/removeFromCart")
    public String removeFromCart(@RequestParam("bookId") int bookId,
            Principal principal, HttpSession session, Model model) {
        
        try {
            String username = principal.getName();
            Cart cart = getOrCreateCart(session);
            
            // Todo encapsulado en el servicio
            cartManagerService.removeItemFromCart(username, cart, bookId);
            session.setAttribute(CART_ATTRIBUTE, cart);
            
            model.addAttribute("message", "cart.itemRemoved");
            return "redirect:viewCart";
            
        } catch (Exception e) {
            handleCartError(e, session, model, "Error removing item from cart", false);
            return "redirect:viewCart";
        }
    }
    
    @RequestMapping("private/clearCart")
    public String clearCart(Principal principal, HttpSession session, Model model) {
        
        try {
            String username = principal.getName();
            Cart cart = getOrCreateCart(session);
            
            // Todo encapsulado en el servicio
            cartManagerService.clearCart(username, cart);
            session.setAttribute(CART_ATTRIBUTE, cart);
            
            model.addAttribute("message", "cart.cartCleared");
            model.addAttribute("total", cart.getTotal());
            return "private/viewCart";
            
        } catch (Exception e) {
            handleCartError(e, session, model, "Error clearing cart", false);
            return "private/viewCart";
        }
    }
    
    @RequestMapping("private/purchaseItem")
    public String purchaseItem(@RequestParam("bookId") int bookId,
            Principal principal, HttpSession session) {
        
        try {
            String username = principal.getName();
            Cart cart = getOrCreateCart(session);
            
            // Todo encapsulado en el servicio
            cartManagerService.purchaseItem(username, cart, bookId);
            session.setAttribute(CART_ATTRIBUTE, cart);
            
            session.setAttribute("message", "cart.itemPurchased");
            return "redirect:viewCart";
            
        } catch (Exception e) {
            handleCartError(e, session, null, "Error purchasing item", true);
            return "redirect:viewCart";
        }
    }
    
    @RequestMapping("private/checkout")
    public String checkout(Principal principal, HttpSession session, Model model) {
        
        Cart cart = getOrCreateCart(session);
        
        if (cart.isEmpty()) {
            model.addAttribute("error", "cart.empty");
            return "private/viewCart";
        }
        
        try {
            String username = principal.getName();
            
            // Todo encapsulado en el servicio
            boolean success = cartManagerService.checkout(username, cart);
            
            if (success) {
                model.addAttribute("message", "cart.purchaseSuccess");
                session.removeAttribute(CART_ATTRIBUTE);
                return "private/checkoutSuccess";
            } else {
                model.addAttribute("error", "cart.someItemsOutOfStock");
                return "private/viewCart";
            }
            
        } catch (Exception e) {
            handleCartError(e, session, model, "Error processing checkout", false);
            return "private/error";
        }
    }

      // Método privado auxiliar - Obtiene o crea el carrito de la sesión
      private Cart getOrCreateCart(HttpSession session) {
        Cart cart = (Cart) session.getAttribute(CART_ATTRIBUTE);
        if (cart == null) {
            cart = new Cart();
            session.setAttribute(CART_ATTRIBUTE, cart);
        }
        return cart;
    }
    
    // Método privado auxiliar - Maneja errores estándar del carrito
    private void handleCartError(Exception e, HttpSession session, Model model, String logMessage, boolean useSession) {
        logger.error(logMessage, e);
        
        String errorMsg = e.getMessage();
        String errorKey = "error.general";
        
        // Si el mensaje es una clave de internacionalización válida, usarla
        if (errorMsg != null && (errorMsg.startsWith("cart.") || errorMsg.startsWith("reservation.") || errorMsg.startsWith("error."))) {
            errorKey = errorMsg;
        }
        
        if (useSession) {
            session.setAttribute("error", errorKey);
        } else {
            model.addAttribute("error", errorKey);
        }
    }
}
