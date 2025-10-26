# Documentación de Implementación
## Trabajo 2 - MIW Arquitectura de Sitios Web
## Aplicación AmazinSpring

---

## Índice

1. [Introducción](#introducción)
2. [Requisito 1: Compra de Libros con Control de Stock](#requisito-1-compra-de-libros-con-control-de-stock)
3. [Requisito 2: Sistema de Reservas](#requisito-2-sistema-de-reservas)
4. [Requisito 3: Internacionalización](#requisito-3-internacionalización)
5. [Arquitectura General](#arquitectura-general)
6. [Patrones y Buenas Prácticas Aplicados](#patrones-y-buenas-prácticas-aplicados)

---

## Introducción

Este documento describe la implementación detallada de los requisitos funcionales del Trabajo 2 de la asignatura "Arquitectura de Sitios Web" del Máster en Ingeniería Web (MIW). La aplicación AmazinSpring ha sido extendida siguiendo una arquitectura en capas (Modelo-Vista-Controlador) y respetando los convenios de nombres y patrones establecidos en el piloto original.

La aplicación implementa:
- **Sistema de compra con control de stock**
- **Sistema de reservas con pago parcial**
- **Internacionalización completa (español e inglés)**

---

## Requisito 1: Compra de Libros con Control de Stock

### 1.1. Modificaciones en la Base de Datos

#### 1.1.1. Modelo de Datos - Entidad Book

Se ha añadido el atributo `stock` a la entidad `Book` para llevar el control del inventario:

```java
@Entity
public class Book {
    @Id @GeneratedValue
    private int id;
    private String title;
    private String description;
    private String author;
    @JoinColumn(name = "taxGroup", nullable = false)
    @ManyToOne(optional = false, cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private VAT vat;
    private double basePrice;
    private int stock;  // <-- NUEVO ATRIBUTO
    
    @Transient
    private double price;
    
    // ... getters y setters
}
```

**Características:**
- El atributo `stock` es persistente y se almacena en la base de datos.
- El precio final (`price`) es transitorio y se calcula dinámicamente incluyendo IVA y descuentos.
- La configuración en `persistence.xml` utiliza `jakarta.persistence.schema-generation.database.action = update` para actualizar automáticamente el esquema.

#### 1.1.2. Datos Iniciales

Para cumplir con el requisito de tener al menos 3 libros con 10 unidades iniciales, se configuran los datos de prueba. Los libros se crean con 10 unidades de stock inicial en el arranque de la aplicación.

### 1.2. Implementación del Carrito de Compra

#### 1.2.1. Modelo Cart y CartItem

Se han creado dos clases POJO (Plain Old Java Object) para gestionar el carrito:

**CartItem.java:**
```java
public class CartItem {
    private Book book;
    private int quantity;
    private boolean isReserved;  // Marca para diferenciar reservas de compras
    
    public double getSubtotal() {
        if (isReserved) {
            return book.getPrice() * quantity * 0.95; // 95% restante en reservas
        }
        return book.getPrice() * quantity;
    }
    
    public double getPaidAmount() {
        if (isReserved) {
            return book.getPrice() * quantity * 0.05; // 5% ya pagado
        }
        return 0;
    }
}
```

**Cart.java:**
```java
public class Cart {
    private List<CartItem> items;
    
    public void addItem(Book book, int quantity) {
        // Buscar si el libro ya está en el carrito COMO COMPRA NORMAL
        for (CartItem item : items) {
            if (item.getBook().getId() == book.getId() && !item.isReserved()) {
                item.setQuantity(item.getQuantity() + quantity);
                return;
            }
        }
        // Si no está, crear nuevo CartItem
        items.add(new CartItem(book, quantity, false));
    }
    
    public double getTotal() {
        return items.stream()
            .mapToDouble(CartItem::getSubtotal)
            .sum();
    }
}
```

**Características:**
- `Cart` mantiene una lista de `CartItem` con operaciones para añadir, eliminar y actualizar.
- `CartItem` distingue entre compras normales y reservas mediante el flag `isReserved`.
- Los cálculos de subtotales consideran el tipo de item (compra vs reserva).

#### 1.2.2. Gestión de Sesión - CartSessionService

Se implementa un servicio para gestionar el carrito en la sesión HTTP:

```java
@Service
public class CartSessionService {
    private static final String CART_ATTRIBUTE = "cart";
    
    public Cart getOrCreateCart(HttpSession session) {
        Cart cart = (Cart) session.getAttribute(CART_ATTRIBUTE);
        if (cart == null) {
            cart = new Cart();
            session.setAttribute(CART_ATTRIBUTE, cart);
        }
        return cart;
    }
    
    public void updateCart(HttpSession session, Cart cart) {
        session.setAttribute(CART_ATTRIBUTE, cart);
    }
    
    public void clearCart(HttpSession session) {
        session.removeAttribute(CART_ATTRIBUTE);
    }
}
```

**Ventajas:**
- Centraliza la lógica de acceso al carrito en sesión.
- Evita duplicación de código en los controladores.
- Facilita el testing y mantenimiento.

### 1.3. Verificación de Disponibilidad de Stock

#### 1.3.1. Capa de Persistencia - BookDAO

Se implementan métodos atómicos con bloqueos pesimistas para evitar condiciones de carrera:

```java
@Override
public boolean checkStockAvailability(int bookId, int requestedQuantity) throws Exception {
    Dba dba = new Dba(true); // Solo lectura
    try {
        EntityManager em = dba.getActiveEm();
        Book book = em.find(Book.class, bookId);
        
        if (book != null) {
            return book.getStock() >= requestedQuantity;
        }
        return false;
    } finally {
        dba.closeEm();
    }
}

@Override
public boolean reduceStock(int bookId, int quantity) throws Exception {
    Dba dba = new Dba();
    try {
        EntityManager em = dba.getActiveEm();
        
        // BLOQUEO PESIMISTA para evitar condiciones de carrera
        Book book = em.find(Book.class, bookId, 
                           jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        
        if (book != null && book.getStock() >= quantity) {
            book.setStock(book.getStock() - quantity);
            em.merge(book);
            return true;
        }
        return false;
    } finally {
        dba.closeEm();
    }
}
```

**Características:**
- **Bloqueo pesimista (PESSIMISTIC_WRITE):** Garantiza que solo un hilo puede modificar el stock a la vez.
- **Transacciones atómicas:** La verificación y reducción de stock ocurren en la misma transacción.
- **Manejo robusto de errores:** Siempre se cierra el EntityManager en el bloque `finally`.

#### 1.3.2. Capa de Negocio - CartManager

La lógica de negocio verifica el stock antes de añadir al carrito:

```java
@Override
public void addBookToCart(Cart cart, int bookId, int quantity) throws Exception {
    // 1. Obtener el libro (CON precio calculado)
    Book book = bookManagerService.getBookById(bookId);
    
    if (book == null) {
        throw new Exception("cart.bookNotFound");
    }
    
    // 2. Calcular cantidad total solicitada
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
}
```

**Flujo:**
1. Se obtiene el libro con su precio calculado (IVA + descuentos).
2. Se calcula la cantidad total considerando lo que ya está en el carrito.
3. Se verifica que hay stock suficiente.
4. Solo si todo es correcto, se añade al carrito.

### 1.4. Proceso de Checkout

#### 1.4.1. Sincronización con Contexto de Aplicación

El checkout utiliza el contexto de la aplicación (`ServletContext`) para sincronización global:

```java
@RequestMapping("private/checkout")
public String checkout(Principal principal, HttpSession session, Model model) {
    Cart cart = cartSessionService.getOrCreateCart(session);
    
    if (cart.isEmpty()) {
        model.addAttribute("error", "cart.empty");
        return "private/viewCart";
    }
    
    // SINCRONIZACIÓN USANDO EL CONTEXTO DE LA APLICACIÓN
    synchronized (servletContext) {
        try {
            String username = principal.getName();
            
            // Procesar reservas
            reservationManagerService.processReservationsInCart(username, cart);
            
            // Procesar compras normales
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
```

**Características críticas:**
- **Sincronización global:** `synchronized (servletContext)` asegura que solo un usuario puede hacer checkout a la vez.
- **Verificación en tiempo de compra:** Se verifica nuevamente el stock en el momento del checkout.
- **Procesamiento separado:** Las reservas y compras normales se procesan independientemente.

#### 1.4.2. Reducción de Stock en Checkout

```java
@Override
public boolean processNormalPurchases(Cart cart) throws Exception {
    if (cart == null || cart.isEmpty()) {
        return true;
    }
    
    for (CartItem item : cart.getItems()) {
        // Solo procesar items NO reservados
        if (!item.isReserved()) {
            boolean reduced = reduceStockForPurchase(
                item.getBookId(), 
                item.getQuantity()
            );
            
            if (!reduced) {
                return false;
            }
        }
    }
    return true;
}
```

### 1.5. Vista del Carrito

La vista JSP `viewCart.jsp` muestra todos los elementos del carrito con internacionalización completa:

```jsp
<c:forEach var='item' items="${cart.items}">
    <tr>
        <td><c:out value="${item.title}" /></td>
        <td><c:out value="${item.author}" /></td>
        <td>
            <c:choose>
                <c:when test="${item.reserved}">
                    <span style="color: orange; font-weight: bold;">
                        <spring:message code="cart.reservation"/> (95% <spring:message code="cart.pending"/>)
                    </span>
                </c:when>
                <c:otherwise>
                    <spring:message code="cart.purchase"/>
                </c:otherwise>
            </c:choose>
        </td>
        <td><c:out value="${item.unitPrice}" /> &euro;</td>
        <td><c:out value="${item.quantity}" /></td>
        <td><c:out value="${item.subtotal}" /> &euro;</td>
        <td>
            <form action="purchaseItem" method="post" style="display:inline;">
                <input type="hidden" name="bookId" value="${item.bookId}"/>
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                <input type="submit" value="<spring:message code='cart.buyItem'/>"/>
            </form>
            <form action="removeFromCart" method="post" style="display:inline;">
                <input type="hidden" name="bookId" value="${item.bookId}"/>
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                <input type="submit" value="<spring:message code='cart.remove'/>"/>
            </form>
        </td>
    </tr>
</c:forEach>
```

**Características:**
- Muestra título, autor, tipo (compra/reserva), precio unitario, cantidad y subtotal.
- Permite comprar o eliminar items individualmente.
- Indica visualmente las reservas con color naranja y texto explicativo.
- Muestra el total calculado correctamente.

---

## Requisito 2: Sistema de Reservas

### 2.1. Modelo de Datos - Entidad Reservation

Se ha creado una nueva entidad JPA para persistir las reservas:

```java
@Entity
public class Reservation {
    @Id 
    @GeneratedValue
    private int id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;
    
    private String username;
    private int quantity;
    private LocalDateTime reservationDate;
    
    // Métodos calculados dinámicamente
    public double getReservationPrice() {
        return book.getPrice() * quantity * 0.05; // 5% pagado
    }
    
    public double getRemainingAmount() {
        return book.getPrice() * quantity * 0.95; // 95% restante
    }
    
    public double getTotalPrice() {
        return book.getPrice() * quantity;
    }
}
```

**Características:**
- **Relación ManyToOne con Book:** Cada reserva está vinculada a un libro específico.
- **EAGER fetching:** Carga inmediata del libro para evitar problemas de lazy loading.
- **Métodos calculados:** Los precios se calculan dinámicamente, no se persisten.
- **username:** Identifica al usuario que hizo la reserva (integrado con Spring Security).

### 2.2. Creación de Reservas

#### 2.2.1. Capa de Persistencia - ReservationDAO

```java
@Override
public Reservation createReservation(String username, int bookId, int quantity) throws Exception {
    Dba dba = new Dba();
    try {
        EntityManager em = dba.getActiveEm();
        
        // Obtener el Book en el MISMO contexto de persistencia
        Book book = em.find(Book.class, bookId);
        if (book == null) {
            throw new Exception("Book not found: " + bookId);
        }
        
        // Crear la reserva
        Reservation reservation = new Reservation();
        reservation.setBook(book);
        reservation.setUsername(username);
        reservation.setQuantity(quantity);
        reservation.setReservationDate(java.time.LocalDateTime.now());
        
        em.persist(reservation);
        return reservation;
    } finally {
        dba.closeEm();
    }
}
```

**Aspectos importantes:**
- El `Book` se obtiene en el mismo contexto de persistencia para evitar problemas con entidades detached.
- La transacción se completa automáticamente al cerrar el EntityManager.

#### 2.2.2. Capa de Negocio - ReservationManager

```java
@Override
public Reservation createReservation(String username, int bookId, int quantity) throws Exception {
    // 1. Verificar que el libro existe
    Book book = bookManagerService.getBookById(bookId);
    if (book == null) {
        throw new Exception("reservation.bookNotFound");
    }
    
    // 2. Verificar stock disponible
    if (!bookManagerService.checkStockAvailability(bookId, quantity)) {
        throw new Exception("reservation.notEnoughStock");
    }
    
    // 3. Reducir el stock (la reserva bloquea unidades)
    boolean stockReduced = bookManagerService.reduceStock(bookId, quantity);
    if (!stockReduced) {
        throw new Exception("reservation.stockReductionFailed");
    }
    
    // 4. Crear la reserva en la BD
    try {
        Reservation reservation = reservationDataService.createReservation(username, bookId, quantity);
        
        // 5. Actualizar el precio del libro en la reserva
        reservation.getBook().setPrice(book.getPrice());
        
        return reservation;
    } catch (Exception e) {
        // Si falla, restaurar el stock
        bookManagerService.increaseStock(bookId, quantity);
        throw e;
    }
}
```

**Flujo transaccional:**
1. Se verifica que el libro existe y hay stock suficiente.
2. Se reduce el stock inmediatamente (las reservas bloquean unidades).
3. Se crea la reserva en la base de datos.
4. Si algo falla, se restaura el stock mediante rollback compensatorio.

### 2.3. Integración con el Carrito

#### 2.3.1. Añadir Reserva al Carrito

Cuando se crea una reserva, automáticamente se añade al carrito con marca especial:

```java
@RequestMapping("private/reserveBook")
public String reserveBook(
        @RequestParam("bookId") int bookId,
        @RequestParam("quantity") int quantity,
        Principal principal,
        HttpSession session,
        Model model) {
    
    synchronized (servletContext) {
        try {
            String username = principal.getName();
            
            // Verificar si ya existe una reserva
            Reservation existingReservation = reservationManagerService
                .getReservationByUserAndBook(username, bookId);
            
            if (existingReservation != null) {
                // Ya existe, incrementar cantidad
                Reservation updated = reservationManagerService
                    .incrementReservationQuantity(existingReservation.getId(), quantity);
                
                // Actualizar en carrito
                Cart cart = (Cart) session.getAttribute("cart");
                if (cart != null) {
                    for (CartItem item : cart.getItems()) {
                        if (item.getBookId() == bookId && item.isReserved()) {
                            item.setQuantity(updated.getQuantity());
                            break;
                        }
                    }
                }
                
                model.addAttribute("message", "reservation.updated");
                return "redirect:viewCart";
            }
            
            // No existe, crear nueva
            Reservation reservation = reservationManagerService
                .createReservation(username, bookId, quantity);
            
            // Añadir al carrito con marca de reserva
            Cart cart = (Cart) session.getAttribute("cart");
            if (cart == null) {
                cart = new Cart();
            }
            
            CartItem item = new CartItem(reservation.getBook(), quantity, true); // isReserved = true
            cart.getItems().add(item);
            session.setAttribute("cart", cart);
            
            session.setAttribute("message", "reservation.created");
            return "redirect:showBooks";
            
        } catch (Exception e) {
            session.setAttribute("error", "error.general");
            return "redirect:showBooks";
        }
    }
}
```

**Características:**
- Si ya existe una reserva del mismo libro, incrementa la cantidad.
- La reserva aparece en el carrito marcada visualmente.
- Utiliza sincronización para evitar condiciones de carrera.

#### 2.3.2. Sincronización Carrito-Base de Datos

Se implementa un método que sincroniza el carrito con las reservas de la BD:

```java
@Override
public void synchronizeCartWithReservations(Cart cart, List<Reservation> reservations) {
    // 1. Eliminar del carrito reservas que ya no existen en BD
    cart.getItems().removeIf(item -> {
        if (item.isReserved()) {
            boolean existsInDB = reservations.stream()
                .anyMatch(r -> r.getBook().getId() == item.getBookId());
            return !existsInDB;
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
}
```

**Ventajas:**
- La base de datos es la "fuente de verdad" para las reservas.
- Si un usuario elimina una reserva desde "Mis Reservas", desaparece del carrito.
- Si añade una reserva desde otro dispositivo, aparece en el carrito.

### 2.4. Sección "Mis Reservas"

#### 2.4.1. Controlador - ReservationController

```java
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
```

#### 2.4.2. Vista - myReservations.jsp

La vista muestra todas las reservas del usuario con opciones de comprar o eliminar:

```jsp
<table>
    <thead>
    <tr>
        <th><spring:message code="cart.title"/></th>
        <th><spring:message code="cart.author"/></th>
        <th><spring:message code="cart.unitPrice"/></th>
        <th><spring:message code="cart.quantity"/></th>
        <th><spring:message code="reservation.paidAmount"/> (5%)</th>
        <th><spring:message code="reservation.remainingAmount"/> (95%)</th>
        <th><spring:message code="reservation.date"/></th>
        <th><spring:message code="reservation.actions"/></th>
    </tr>
    </thead>
    <tbody>
        <c:forEach var='reservation' items="${reservations}">
            <tr>
                <td><c:out value="${reservation.book.title}" /></td>
                <td><c:out value="${reservation.book.author}" /></td>
                <td><c:out value="${reservation.book.price}" /> &euro;</td>
                <td><c:out value="${reservation.quantity}" /></td>
                <td><c:out value="${reservation.reservationPrice}" /> &euro;</td>
                <td><c:out value="${reservation.remainingAmount}" /> &euro;</td>
                <td><c:out value="${reservation.reservationDate}" /></td>
                <td>
                    <form action="purchaseReservation" method="post" style="display:inline;">
                        <input type="hidden" name="reservationId" value="${reservation.id}"/>
                        <input type="submit" value="<spring:message code='reservation.buy'/>"/>
                    </form>
                    <form action="cancelReservationFromPage" method="post" style="display:inline;">
                        <input type="hidden" name="reservationId" value="${reservation.id}"/>
                        <input type="submit" value="<spring:message code='reservation.cancel'/>" 
                               onclick="return confirm(this.getAttribute('data-confirm'))"/>
                    </form>
                </td>
            </tr>
        </c:forEach>
    </tbody>
</table>
```

**Información mostrada:**
- Título y autor del libro.
- Precio unitario actual.
- Cantidad reservada.
- Monto pagado (5% del total).
- Monto pendiente (95% del total).
- Fecha de la reserva.
- Botones para comprar o eliminar.

### 2.5. Compra de Reservas

#### 2.5.1. Comprar desde "Mis Reservas"

```java
@RequestMapping("private/purchaseReservation")
public String purchaseReservation(
        @RequestParam("reservationId") int reservationId,
        Principal principal,
        HttpSession session,
        Model model) {
    
    synchronized (servletContext) {
        try {
            String username = principal.getName();
            
            // 1. Obtener información antes de eliminar
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
```

**Flujo:**
1. Se obtiene la información de la reserva antes de eliminarla.
2. Se elimina la reserva de la base de datos (el stock ya estaba reducido desde la creación).
3. Se elimina del carrito de sesión.
4. Se muestra mensaje de confirmación.

#### 2.5.2. Comprar desde el Carrito

El usuario también puede comprar una reserva directamente desde el carrito usando el método `purchaseItem`:

```java
if (itemToPurchase.isReserved()) {
    // Es reserva: completar pago del 95% restante
    Reservation res = reservationManagerService.getReservationByUserAndBook(username, bookId);
    
    if (res != null) {
        reservationManagerService.purchaseReservation(res.getId());
        logger.debug("Reservation purchased: " + res.getId());
    }
} else {
    // Es compra normal: reducir stock
    boolean reduced = cartManagerService.reduceStockForPurchase(
        itemToPurchase.getBookId(), 
        itemToPurchase.getQuantity()
    );
}
```

### 2.6. Cancelación de Reservas

#### 2.6.1. Lógica de Restauración de Stock

```java
@Override
public boolean cancelReservation(int reservationId) throws Exception {
    // 1. Obtener la reserva
    Reservation reservation = reservationDataService.getReservationById(reservationId);
    
    if (reservation == null) {
        throw new Exception("reservation.notFound");
    }
    
    // 2. Restaurar el stock antes de eliminar
    Book book = reservation.getBook();
    bookManagerService.increaseStock(book.getId(), reservation.getQuantity());
    
    // 3. Eliminar la reserva
    reservationDataService.deleteReservation(reservationId);
    
    return true;
}
```

**Características:**
- Cuando se cancela una reserva, las unidades vuelven a estar disponibles.
- Se garantiza que el stock se restaura antes de eliminar la reserva.
- Si falla la restauración, la excepción previene la eliminación de la reserva.

#### 2.6.2. Método Atómico para Incrementar Stock

```java
@Override
public void increaseBookStock(int bookId, int quantity) throws Exception {
    Dba dba = new Dba();
    try {
        EntityManager em = dba.getActiveEm();
        
        // Usar bloqueo pesimista
        Book book = em.find(Book.class, bookId, 
                           jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        
        if (book != null) {
            int newStock = book.getStock() + quantity;
            book.setStock(newStock);
            em.merge(book);
        } else {
            throw new Exception("Book with ID " + bookId + " not found");
        }
    } finally {
        dba.closeEm();
    }
}
```

---

## Requisito 3: Internacionalización

### 3.1. Configuración de Internacionalización

#### 3.1.1. LocaleResolver - servlet-context.xml

Se configura el `SessionLocaleResolver` para gestionar el idioma en la sesión:

```xml
<!-- Configuración de LocaleResolver para gestión de idiomas -->
<beans:bean id="localeResolver" 
            class="org.springframework.web.servlet.i18n.SessionLocaleResolver">
    <beans:property name="defaultLocale" value="es" />
</beans:bean>
```

**Características:**
- El locale se almacena en la sesión HTTP del usuario.
- El idioma por defecto es español (`es`).
- El cambio de idioma persiste durante toda la sesión.

#### 3.1.2. Archivos de Mensajes

Se han creado dos archivos de propiedades en `src/main/resources/`:

**messages.properties (Español):**
```properties
welcome=Bienvenido a <em>la más pequeña</em> tienda virtual del mundo!
cart.addToCart=Añadir al carrito
cart.shoppingCart=Carrito de compra
cart.total=Total
cart.purchaseSuccess=¡Compra realizada con éxito!
reservation.myReservations=Mis Reservas
reservation.created=Reserva creada exitosamente
language.select=Idioma
# ... más de 100 claves traducidas
```

**messages_en.properties (Inglés):**
```properties
welcome=Welcome to the <em>smallest</em> virtual shop in the world!!!
cart.addToCart=Add to Cart
cart.shoppingCart=Shopping Cart
cart.total=Total
cart.purchaseSuccess=Purchase Successful!
reservation.myReservations=My Reservations
reservation.created=Reservation created successfully
language.select=Language
# ... más de 100 claves traducidas
```

**Cobertura completa:**
- Mensajes de carrito y compra.
- Mensajes de reservas.
- Navegación y menús.
- Formularios.
- Mensajes de error y validación.
- Labels de campos.

### 3.2. Selector de Idioma

#### 3.2.1. Componente Reutilizable - languageSelector.jsp

Se ha creado un componente JSP reutilizable incluido en todas las páginas:

```jsp
<script type="text/javascript">
function changeLanguage(lang) {
    var contextPath = '${pageContext.request.contextPath}';
    window.location.href = contextPath + '/changeLanguage?lang=' + lang;
}
</script>

<div style="text-align: right; padding: 10px; margin-bottom: 10px;">
    <label for="languageSelect"><spring:message code="language.select"/>: </label>
    <select id="languageSelect" onchange="changeLanguage(this.value)" 
            style="padding: 5px; font-size: 14px;">
        <option value="es" ${pageContext.response.locale.language == 'es' ? 'selected' : ''}>
            Español
        </option>
        <option value="en" ${pageContext.response.locale.language == 'en' ? 'selected' : ''}>
            English
        </option>
    </select>
</div>
```

**Características:**
- **Sin botones:** El evento `onchange` del `<select>` captura automáticamente el cambio.
- **Mantiene el contexto:** Redirige a la misma página después del cambio.
- **Selección visual:** Muestra el idioma activo como seleccionado.
- **Consistente:** Se incluye en todas las páginas con `<jsp:include page="../languageSelector.jsp" />`.

#### 3.2.2. Controlador de Cambio de Idioma - LanguageController

```java
@Controller
public class LanguageController {
    
    @Autowired
    private LocaleResolver localeResolver;
    
    @GetMapping("/changeLanguage")
    public String changeLanguage(
            @RequestParam("lang") String lang,
            HttpServletRequest request, 
            HttpServletResponse response) {
        
        // Establecer el nuevo locale
        Locale locale = new Locale(lang);
        localeResolver.setLocale(request, response, locale);
        
        // Redirigir a la página anterior (Referer)
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            return "redirect:" + referer;
        }
        
        // Si no hay referer, redirigir al menú principal
        return "redirect:/private/menu";
    }
}
```

**Flujo:**
1. Se recibe el parámetro `lang` (es o en).
2. Se crea un `Locale` y se establece en la sesión mediante el `LocaleResolver`.
3. Se redirige a la página anterior usando el header `Referer`.
4. Si no hay referer, se redirige al menú principal.

#### 3.2.3. Configuración de Seguridad

Se permite el acceso al controlador de idioma sin autenticación:

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
    .authorizeHttpRequests((requests) -> requests
        .requestMatchers("/private/**", "/main/**").hasRole("ADMIN")
        .requestMatchers("/resources/**", "/changeLanguage").permitAll()  // <-- PERMITE cambio de idioma
        .anyRequest().authenticated()
    )
    // ...
    return http.build();
}
```

### 3.3. Uso en Vistas JSP

#### 3.3.1. Etiquetas Spring

Todas las vistas utilizan la etiqueta `<spring:message>` para internacionalización:

```jsp
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring"%>

<h1><spring:message code="cart.shoppingCart"/></h1>

<table>
    <thead>
        <tr>
            <th><spring:message code="cart.title"/></th>
            <th><spring:message code="cart.author"/></th>
            <th><spring:message code="cart.quantity"/></th>
            <th><spring:message code="cart.total"/></th>
        </tr>
    </thead>
</table>

<input type="submit" value="<spring:message code='cart.completePurchase'/>" />
```

#### 3.3.2. Mensajes Dinámicos

Los mensajes de éxito y error también se internacionalizan:

```jsp
<c:if test="${not empty sessionScope.message}">
    <div style="background-color: #d4edda; ...">
        <spring:message code="${sessionScope.message}"/>
    </div>
    <c:remove var="message" scope="session"/>
</c:if>
```

En el controlador:
```java
session.setAttribute("message", "cart.bookAddedSuccessfully");
```

---

## Arquitectura General

### 4.1. Patrón Arquitectónico: Modelo-Vista-Controlador (MVC)

La aplicación sigue estrictamente el patrón MVC en capas:

```
┌─────────────────────────────────────────────────────┐
│               CAPA DE PRESENTACIÓN                  │
│  (Controllers: CartController, ReservationController)│
│              ↓ (Model + View)                        │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│              CAPA DE LÓGICA DE NEGOCIO              │
│    (Managers: CartManager, ReservationManager,      │
│              BookManager)                           │
│              ↓ (Business Rules)                      │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│             CAPA DE PERSISTENCIA                    │
│       (DAOs: BookDAO, ReservationDAO, VATDAO)       │
│              ↓ (JPA + Hibernate)                     │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│                BASE DE DATOS                        │
│          (HSQLDB: Books, Reservations, VAT)         │
└─────────────────────────────────────────────────────┘
```

### 4.2. Inyección de Dependencias

Se utiliza Spring Framework para la inyección de dependencias:

```java
@Controller
public class CartController {
    
    @Autowired
    private CartManagerService cartManagerService;
    
    @Autowired
    private ReservationManagerService reservationManagerService;
    
    @Autowired
    private CartSessionService cartSessionService;
    
    @Autowired
    private ServletContext servletContext;
}
```

**Ventajas:**
- **Desacoplamiento:** Los controladores no crean instancias de servicios.
- **Testabilidad:** Fácil inyectar mocks para testing.
- **Configuración centralizada:** Toda la configuración está en XML de Spring.

### 4.3. Gestión de Transacciones

#### 4.3.1. Clase Dba (Database Access)

```java
public class Dba {
    private EntityManager em;
    private EntityTransaction et;
    
    public Dba() {
        this(false);
    }
    
    public Dba(boolean readOnly) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("JPA_PU");
        em = emf.createEntityManager();
        if (!readOnly) {
            et = em.getTransaction();
            et.begin();
        }
    }
    
    public EntityManager getActiveEm() {
        return em;
    }
    
    public void closeEm() {
        if (et != null && et.isActive()) {
            et.commit();
        }
        if (em != null && em.isOpen()) {
            em.close();
        }
    }
}
```

**Características:**
- Encapsula la gestión del `EntityManager`.
- Soporte para transacciones de solo lectura.
- El `finally` en los DAOs garantiza que siempre se cierra.

### 4.4. Seguridad

#### 4.4.1. Spring Security Configuration

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
        .authorizeHttpRequests((requests) -> requests
            .requestMatchers("/private/**", "/main/**").hasRole("ADMIN")
            .requestMatchers("/resources/**", "/changeLanguage").permitAll()
            .anyRequest().authenticated()
        )
        .formLogin(form -> form
            .loginPage("/loginForm")
            .loginProcessingUrl("/login")
            .defaultSuccessUrl("/", true)
            .failureUrl("/loginForm?error=wc")
            .permitAll()
        );
        
        return http.build();
    }
    
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails admin = User.builder()
            .username("admin")
            .password(passwordEncoder.encode("amazin"))
            .roles("ADMIN")
            .build();
        
        return new InMemoryUserDetailsManager(admin);
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**Seguridad implementada:**
- Autenticación basada en formulario personalizado.
- Contraseñas encriptadas con BCrypt.
- Protección CSRF habilitada (tokens en formularios).
- Rutas privadas protegidas con rol ADMIN.
- Integración con `Principal` para obtener el usuario autenticado.

---

## Patrones y Buenas Prácticas Aplicados

### 5.1. Patrones de Diseño

#### 5.1.1. Service Layer (Capa de Servicios)

Separación entre controladores y lógica de negocio:
- `CartManagerService` / `CartManager`
- `ReservationManagerService` / `ReservationManager`
- `BookManagerService` / `BookManager`

#### 5.1.2. Data Access Object (DAO)

Encapsulación del acceso a datos:
- `BookDataService` / `BookDAO`
- `ReservationDataService` / `ReservationDAO`
- `VATDataService` / `VATDAO`

#### 5.1.3. Dependency Injection

Uso de Spring IoC para gestión de dependencias.

#### 5.1.4. Session Facade

`CartSessionService` encapsula el acceso al carrito en sesión.

### 5.2. Prácticas de Concurrencia

#### 5.2.1. Bloqueos Pesimistas (Pessimistic Locking)

```java
Book book = em.find(Book.class, bookId, 
                   jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
```

Previene condiciones de carrera al actualizar el stock.

#### 5.2.2. Sincronización con Contexto de Aplicación

```java
synchronized (servletContext) {
    // Operaciones críticas de checkout
}
```

Garantiza que solo un usuario puede hacer checkout a la vez.

### 5.3. Manejo de Errores

#### 5.3.1. Excepciones con Claves Internacionalizadas

```java
throw new Exception("cart.notEnoughStock");
```

Los mensajes de error se traducen automáticamente.

#### 5.3.2. Try-Finally para Recursos

```java
try {
    EntityManager em = dba.getActiveEm();
    // ... operaciones
} finally {
    dba.closeEm(); // Siempre se ejecuta
}
```

### 5.4. Logging

Uso de Log4j2 en todas las capas:

```java
Logger logger = LogManager.getLogger(this.getClass());
logger.debug("Adding book " + bookId + " to cart. Quantity: " + quantity);
logger.error("Failed to reduce stock for book " + bookId, e);
```

### 5.5. Separación de Responsabilidades

#### 5.5.1. Modelos POJO vs Entidades JPA

- `Cart` y `CartItem`: POJOs en sesión (no persistentes).
- `Book` y `Reservation`: Entidades JPA (persistentes).

#### 5.5.2. Cálculos Dinámicos vs Persistencia

- Precios finales: Calculados dinámicamente (`@Transient`).
- Stock: Persistido y actualizado transaccionalmente.

### 5.6. Principios SOLID

#### 5.6.1. Single Responsibility

Cada clase tiene una responsabilidad única:
- `CartManager`: Lógica del carrito.
- `BookDAO`: Acceso a datos de libros.
- `CartSessionService`: Gestión de sesión.

#### 5.6.2. Dependency Inversion

Los controladores dependen de interfaces, no de implementaciones:
- `CartManagerService` (interfaz) → `CartManager` (implementación)

#### 5.6.3. Open/Closed

Las clases están abiertas para extensión pero cerradas para modificación.

---

## Conclusiones

La aplicación AmazinSpring ha sido exitosamente extendida cumpliendo todos los requisitos funcionales:

### ✅ Requisito 1: Compra de Libros con Control de Stock
- Control de stock implementado en la entidad `Book`.
- Carrito de compra funcional con visualización de título, cantidad y total.
- Verificación de disponibilidad en tiempo real.
- Prevención de condiciones de carrera mediante bloqueos pesimistas.
- Actualización atómica del stock en el checkout.

### ✅ Requisito 2: Sistema de Reservas
- Entidad `Reservation` persistente con relación a `Book`.
- Reservas marcan el 5% del precio como pagado.
- Las reservas reducen el stock inmediatamente.
- Sección "Mis Reservas" con opciones de comprar o eliminar.
- Restauración automática del stock al cancelar.
- Sincronización entre carrito de sesión y base de datos.

### ✅ Requisito 3: Internacionalización
- Soporte completo para español e inglés.
- Selector de idioma sin botones (evento onchange).
- Más de 100 claves traducidas en ambos idiomas.
- Cambio de idioma mantiene el contexto de navegación.
- Mensajes dinámicos internacionalizados.

### Calidad de Implementación

- **Arquitectura limpia:** MVC en 3 capas bien definidas.
- **Código mantenible:** Separación de responsabilidades clara.
- **Seguridad robusta:** Spring Security con encriptación BCrypt.
- **Transacciones atómicas:** Uso correcto de JPA y bloqueos.
- **Logging completo:** Trazabilidad de todas las operaciones.
- **Manejo de errores:** Excepciones con rollback compensatorio.
- **Testing-friendly:** Inyección de dependencias facilita el testing.

La implementación respeta completamente la arquitectura y convenios del piloto original, extendiendo la funcionalidad de manera coherente y profesional.

---

## Anexos

### Tecnologías Utilizadas

- **Framework MVC:** Spring Framework 6.0.12
- **Seguridad:** Spring Security 6.0.0
- **ORM:** Hibernate 5.6.15 (Jakarta Persistence API 3.2)
- **Base de Datos:** HSQLDB 2.6.1
- **Servidor de Aplicaciones:** Tomcat (o compatible con Servlets 5.0+)
- **Vista:** JSP con JSTL 2.0
- **Logging:** Log4j2 2.16.0
- **Build Tool:** Maven

### Estructura de Directorios

```
src/main/
├── java/com/miw/
│   ├── business/
│   │   ├── bookmanager/
│   │   ├── cartmanager/
│   │   └── reservationmanager/
│   ├── config/
│   │   └── SecurityConfig.java
│   ├── model/
│   │   ├── Book.java
│   │   ├── Cart.java
│   │   ├── CartItem.java
│   │   └── Reservation.java
│   ├── persistence/
│   │   ├── book/
│   │   └── reservation/
│   └── presentation/
│       ├── CartController.java
│       ├── LanguageController.java
│       └── ReservationController.java
├── resources/
│   ├── messages.properties
│   ├── messages_en.properties
│   └── META-INF/persistence.xml
└── webapp/
    ├── resources/css/
    └── WEB-INF/
        ├── views/
        │   ├── languageSelector.jsp
        │   └── private/
        │       ├── viewCart.jsp
        │       ├── showBooks.jsp
        │       └── myReservations.jsp
        └── spring/
            └── appServlet/
                └── servlet-context.xml
```

---

**Documento generado para el Trabajo 2 - MIW Arquitectura de Sitios Web**  
**Universidad de Oviedo - Máster en Ingeniería Web**

