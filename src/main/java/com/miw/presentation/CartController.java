package com.miw.presentation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import com.miw.business.cartmanager.CartManagerService;
import com.miw.model.Cart;

@Controller
public class CartController {
    
    @Autowired
    private CartManagerService cartManagerService;
    
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
                session.setAttribute("cart", cart);
            }
            
            // Añadir libro al carrito
            cartManagerService.addBookToCart(cart, bookId, quantity);
            
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
    public String viewCart(HttpSession session, Model model) {
        
        Cart cart = (Cart) session.getAttribute("cart");
        if (cart == null) {
            cart = new Cart();
            session.setAttribute("cart", cart);
        }
        
        model.addAttribute("cart", cart);
        model.addAttribute("total", cart.getTotal());
        
        return "private/viewCart";
    }
    
    @RequestMapping("private/clearCart")
    public String clearCart(HttpSession session, Model model) {
        
        try {
            // Obtener el carrito de la sesión
            Cart cart = (Cart) session.getAttribute("cart");
            
            if (cart != null) {
                // Vaciar el carrito
                cart.clear();
                session.setAttribute("cart", cart);
                
                // Mensaje de éxito
                model.addAttribute("message", "cart.cartCleared");
            } else {
                // Si no hay carrito, crear uno vacío
                cart = new Cart();
                session.setAttribute("cart", cart);
            }
            
            // Preparar el modelo para la vista
            model.addAttribute("cart", cart);
            model.addAttribute("total", cart.getTotal());
            
            return "private/viewCart";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "private/viewCart";
        }
    }
    
    @RequestMapping("private/checkout")
    public String checkout(HttpSession session, Model model) {
        
        Cart cart = (Cart) session.getAttribute("cart");
        
        if (cart == null || cart.getItems().isEmpty()) {
            model.addAttribute("error", "cart.empty");
            return "private/viewCart";
        }
        
        // SINCRONIZACIÓN USANDO EL CONTEXTO DE LA APLICACIÓN
        // Esto previene que dos usuarios compren el mismo stock simultáneamente
        synchronized (servletContext) {
            try {
                boolean success = cartManagerService.checkout(cart);
                
                if (success) {
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
