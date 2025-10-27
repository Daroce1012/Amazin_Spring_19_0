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
    
    private static final Logger logger = LogManager.getLogger(CartManager.class);
    
    @Autowired
    private BookManagerService bookManagerService;
    
    @Autowired
    private ReservationManagerService reservationManagerService;
    
    // Método privado - Procesa la compra de un item (reserva o normal)
    private void processPurchaseForItem(String username, int bookId, int quantity, boolean isReserved) throws Exception {
        if (isReserved) {
            // Es reserva: completar pago del 95% restante
            Reservation res = reservationManagerService.getReservationByUserAndBook(username, bookId);
            
            if (res == null) {
                throw new Exception("reservation.notFound");
            }
            
            reservationManagerService.purchaseReservation(res.getId(), username);
            logger.debug("Reservation purchased: {}", res.getId());
        } else {
            // Es compra normal: reducir stock
            logger.debug("Reducing stock for purchase: book {}, quantity: {}", bookId, quantity);
            boolean reduced = bookManagerService.reduceStock(bookId, quantity);
            
            if (!reduced) {
                throw new Exception("cart.notEnoughStock");
            }
            logger.debug("Stock reduced for normal purchase");
        }
    }
    
    // ==================== MÉTODOS PÚBLICOS ==================== 
    @Override
    public void addBookToCart(Cart cart, int bookId, int quantity) throws Exception {
        logger.debug("Adding book {} to cart. Quantity: {}", bookId, quantity);
        
        // 1. Obtener el libro (CON precio calculado por BookManager)
        Book book = bookManagerService.getBookById(bookId);
        
        if (book == null) {
            throw new Exception("cart.bookNotFound");
        }
        
        // 2. Calcular cantidad total solicitada (considerando lo que ya está en el carrito)
        // IMPORTANTE: Solo contar items NO reservados, porque las reservas ya redujeron stock
        int totalRequested = quantity;
        CartItem existingItem = cart.findItemByBookId(bookId, false);
        if (existingItem != null) {
            totalRequested += existingItem.getQuantity();
        }
        
        // 3. Verificar stock disponible
        if (!bookManagerService.checkStockAvailability(bookId, totalRequested)) {
            throw new Exception("cart.notEnoughStock");
        }
        
        // 4. Añadir al carrito
        cart.addItem(book, quantity, false);
        logger.debug("Book added to cart successfully");
    }
    
    @Override
    public void synchronizeCartForUser(String username, Cart cart) throws Exception {
        logger.debug("Synchronizing cart for user: {}", username);
        
        try {
            // Obtener reservas del usuario
            List<Reservation> reservations = reservationManagerService.getReservations(username);
            
            logger.debug("Synchronizing cart with {} reservations", reservations.size());
            
            // 1. Eliminar todas las reservas del carrito
            cart.removeAllItems(true);
            
            // 2. Agregar todas las reservas desde BD (la BD es la fuente de verdad)
            for (Reservation res : reservations) {
                cart.addItem(res.getBook(), res.getQuantity(), true);
            }
            
            logger.debug("Cart synchronized successfully");
        } catch (Exception e) {
            logger.error("Error synchronizing cart for user: {}", username, e);
            throw new Exception("cart.syncFailed");
        }
    }
    
    @Override
    public void removeItemFromCart(String username, Cart cart, int bookId, boolean isReserved) throws Exception {
        logger.debug("Removing item from cart: bookId={}, isReserved={}, user={}", bookId, isReserved, username);
        
        // Buscar el item específico en el carrito (por ID y tipo)
        CartItem itemToRemove = cart.findItemByBookId(bookId, isReserved);
        
        if (itemToRemove == null) {
            throw new Exception("cart.itemNotFound");
        }
        
        // Si es reserva, cancelarla en BD (restaura stock)
        if (isReserved) {
            boolean cancelled = reservationManagerService.cancelReservationByUserAndBook(username, bookId);
            
            if (!cancelled) {
                logger.warn("Could not cancel reservation for bookId: {}", bookId);
                throw new Exception("reservation.cancelFailed");
            }
        }
        
        // Quitar del carrito
        cart.removeItem(bookId, isReserved);
        
        logger.debug("Item removed from cart successfully");
    }
    
    @Override
    public void clearCart(String username, Cart cart) throws Exception {
        logger.debug("Clearing cart for user: {}", username);
        
        if (!cart.isEmpty()) {
            try {
                // Cancelar todas las reservas
                reservationManagerService.cancelAllReservationsInCart(username, cart);
            } catch (Exception e) {
                logger.error("Error cancelling reservations while clearing cart", e);
                throw new Exception("cart.clearReservationsFailed");
            }
            
            // Vaciar el carrito
            cart.clear();
            
            logger.debug("Cart cleared successfully");
        }
    }
    
    @Override
    public void purchaseItem(String username, Cart cart, int bookId, boolean isReserved) throws Exception {
        logger.debug("Purchasing item: bookId={}, isReserved={}, user={}", bookId, isReserved, username);
        
        // Buscar el item específico en el carrito (por ID y tipo)
        CartItem itemToPurchase = cart.findItemByBookId(bookId, isReserved);
        
        if (itemToPurchase == null) {
            throw new Exception("cart.itemNotFound");
        }
        
        int quantity = itemToPurchase.getQuantity();
        
        // Procesar la compra (maneja reservas y compras normales)
        logger.debug("Purchasing cart item: bookId={}, isReserved={}", bookId, isReserved);
        processPurchaseForItem(username, bookId, quantity, isReserved);
        
        // Quitar del carrito
        cart.removeItem(bookId, isReserved);
        
        logger.debug("Item purchased successfully");
    }
    
    @Override
    public boolean checkout(String username, Cart cart) throws Exception {
        logger.debug("Processing checkout for user: {}", username);
        
        // Procesar reservas
        reservationManagerService.processReservationsInCart(username, cart);
        
        // Procesar compras normales
        logger.debug("Processing normal purchases from cart");
        
        if (cart != null && !cart.isEmpty()) {
            for (CartItem item : cart.getItems()) {
                // Solo procesar items NO reservados
                if (!item.isReserved()) {
                    try {
                        logger.debug("Reducing stock for purchase: book {}, quantity: {}", item.getBookId(), item.getQuantity());
                        boolean reduced = bookManagerService.reduceStock(item.getBookId(), item.getQuantity());
                        
                        if (!reduced) {
                            logger.error("Failed to reduce stock for book: {}", item.getBookId());
                            return false;
                        }
                    } catch (Exception e) {
                        logger.error("Error processing item in checkout", e);
                        return false;
                    }
                }
            }
        }
        
        logger.debug("Checkout completed successfully");
        return true;
    }
}
