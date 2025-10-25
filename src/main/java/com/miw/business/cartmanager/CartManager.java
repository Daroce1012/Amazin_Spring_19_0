package com.miw.business.cartmanager;

import org.apache.logging.log4j.*;
import com.miw.model.Cart;
import com.miw.model.Book;
import com.miw.model.CartItem;
import com.miw.business.bookmanager.BookManagerService;

public class CartManager implements CartManagerService {
    
    Logger logger = LogManager.getLogger(this.getClass());
    
    // Usar BookManagerService, NO BookDataService directamente
    private BookManagerService bookManagerService;
    
    // Getter y Setter para inyección de dependencias
    public BookManagerService getBookManagerService() {
        return bookManagerService;
    }
    
    public void setBookManagerService(BookManagerService bookManagerService) {
        this.bookManagerService = bookManagerService;
    }
    
    @Override
    public void addBookToCart(Cart cart, int bookId, int quantity) throws Exception {
        logger.debug("Adding book " + bookId + " to cart. Quantity: " + quantity);
        
        // 1. Obtener el libro (CON precio calculado por BookManager)
        Book book = bookManagerService.getBookById(bookId);
        
        if (book == null) {
            throw new Exception("cart.bookNotFound");
        }
        
        // 2. Calcular cantidad total solicitada (considerando lo que ya está en el carrito)
        int totalRequested = quantity;
        for (CartItem item : cart.getItems()) {
            if (item.getBook().getId() == bookId) {
                totalRequested += item.getQuantity();
                break;
            }
        }
        
        // 3. Verificar stock disponible
        if (!bookManagerService.checkStockAvailability(bookId, totalRequested)) {
            throw new Exception("cart.notEnoughStock");
        }
        
        // 4. Añadir al carrito
        cart.addItem(book, quantity);
        logger.debug("Book added to cart successfully");
    }
    
    @Override
    public Cart getCart() {
        return new Cart(); // O recuperar de sesión
    }
    
    @Override
    public boolean checkout(Cart cart) throws Exception {
        logger.debug("Processing checkout for cart with " + cart.getItems().size() + " items");
        
        boolean allSuccess = true;
        
        // Intentar reducir el stock de todos los items
        for (CartItem item : cart.getItems()) {
            boolean reduced = bookManagerService.reduceStock(
                item.getBook().getId(), 
                item.getQuantity()
            );
            
            if (!reduced) {
                logger.error("Failed to reduce stock for book: " + item.getBook().getTitle());
                allSuccess = false;
                // En producción, aquí deberías hacer rollback de los anteriores
                break;
            }
        }
        
        if (allSuccess) {
            cart.clear(); // Vaciar carrito tras compra exitosa
            logger.debug("Checkout completed successfully");
        }
        
        return allSuccess;
    }
    
    @Override
    public double calculateTotal(Cart cart) {
        return cart.getTotal();
    }
}
