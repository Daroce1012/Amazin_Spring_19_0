package com.miw.business.cartmanager;

import org.springframework.beans.factory.annotation.Autowired;
import org.apache.logging.log4j.*;
import com.miw.model.Cart;
import com.miw.model.Book;
import com.miw.model.CartItem;
import com.miw.model.Reservation;
import com.miw.business.bookmanager.BookManagerService;
import com.miw.business.reservationmanager.ReservationManagerService;
import java.util.List;

public class CartManager implements CartManagerService {
    
    Logger logger = LogManager.getLogger(this.getClass());
    
    @Autowired
    private BookManagerService bookManagerService;
    
    @Autowired
    private ReservationManagerService reservationManagerService;
    
    @Override
    public void addBookToCart(Cart cart, int bookId, int quantity) throws Exception {
        logger.debug("Adding book " + bookId + " to cart. Quantity: " + quantity);
        
        // 1. Obtener el libro (CON precio calculado por BookManager)
        Book book = bookManagerService.getBookById(bookId);
        
        if (book == null) {
            throw new Exception("cart.bookNotFound");
        }
        
        // 2. Calcular cantidad total solicitada (considerando lo que ya está en el carrito)
        // IMPORTANTE: Solo contar items NO reservados, porque las reservas ya redujeron stock
        int totalRequested = quantity;
        for (CartItem item : cart.getItems()) {
            if (item.getBook().getId() == bookId && !item.isReserved()) {
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
    public boolean reduceStockForPurchase(int bookId, int quantity) throws Exception {
        logger.debug("Reducing stock for purchase: book " + bookId + ", quantity: " + quantity);
        return bookManagerService.reduceStock(bookId, quantity);
    }
    
    @Override
    public void synchronizeCartWithReservations(Cart cart, List<Reservation> reservations) {
        logger.debug("Synchronizing cart with " + reservations.size() + " reservations");
        
        // 1. Eliminar del carrito reservas que ya no existen en BD
        cart.getItems().removeIf(item -> {
            if (item.isReserved()) {
                boolean existsInDB = reservations.stream()
                    .anyMatch(r -> r.getBook().getId() == item.getBookId());
                return !existsInDB; // Eliminar si no existe en BD
            }
            return false;
        });
        
        // 2. Agregar o actualizar reservas desde BD
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
        
        logger.debug("Cart synchronized successfully");
    }
    
    @Override
    public boolean processNormalPurchases(Cart cart) throws Exception {
        logger.debug("Processing normal purchases from cart");
        
        if (cart == null || cart.isEmpty()) {
            return true; // Carrito vacío no es un error
        }
        
        for (CartItem item : cart.getItems()) {
            // Solo procesar items NO reservados
            if (!item.isReserved()) {
                try {
                    boolean reduced = reduceStockForPurchase(
                        item.getBookId(), 
                        item.getQuantity()
                    );
                    
                    if (!reduced) {
                        logger.error("Failed to reduce stock for book: " + item.getBookId());
                        return false;
                    }
                } catch (Exception e) {
                    logger.error("Error processing item in checkout", e);
                    return false;
                }
            }
        }
        
        return true;
    }
    
    @Override
    public void purchaseCartItem(String username, int bookId, int quantity, boolean isReserved) throws Exception {
        logger.debug("Purchasing cart item: bookId=" + bookId + ", isReserved=" + isReserved);
        
        if (isReserved) {
            // Es reserva: completar pago del 95% restante
            Reservation res = reservationManagerService.getReservationByUserAndBook(username, bookId);
            
            if (res == null) {
                throw new Exception("reservation.notFound");
            }
            
            reservationManagerService.purchaseReservation(res.getId());
            logger.debug("Reservation purchased: " + res.getId());
        } else {
            // Es compra normal: reducir stock
            boolean reduced = reduceStockForPurchase(bookId, quantity);
            
            if (!reduced) {
                throw new Exception("cart.notEnoughStock");
            }
            logger.debug("Stock reduced for normal purchase");
        }
    }
}
