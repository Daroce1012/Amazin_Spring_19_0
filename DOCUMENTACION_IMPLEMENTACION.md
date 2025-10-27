# Documentación de Implementación - AmazinSpring
**MIW - Arquitectura de Sitios Web - Trabajo 2**

---

## Índice
1. [Introducción](#introducción)
2. [Acceso a la Aplicación](#acceso-a-la-aplicación)
3. [Requisito 1: Compra de Libros](#requisito-1-compra-de-libros)
4. [Requisito 2: Sistema de Reservas](#requisito-2-sistema-de-reservas)
5. [Requisito 3: Internacionalización](#requisito-3-internacionalización)
6. [Arquitectura General](#arquitectura-general)
7. [Consideraciones Técnicas](#consideraciones-técnicas)

---

## Introducción

Este documento describe la implementación completa de las funcionalidades requeridas para el Trabajo 2 de la asignatura Arquitectura de Sitios Web del Máster en Ingeniería Web. La implementación se ha realizado respetando la arquitectura en capas del piloto original (Presentación → Negocio → Persistencia) y manteniendo los convenios de nombres establecidos.

---

## Acceso a la Aplicación

### URL de Acceso

La aplicación está desplegada en una máquina virtual y es accesible a través de la siguiente URL:

**🔗 http://156.35.95.57:8080/Amazin_Spring_19_0/**

### Información de Despliegue

- **Servidor**: Apache Tomcat
- **Puerto**: 8080
- **Contexto**: /Amazin_Spring_19_0
- **Base de Datos**: HSQLDB (hsql://localhost/amazin19)

### Páginas Principales

Una vez desplegada, puedes acceder a las siguientes funcionalidades:

1. **Página de Login**: `http://156.35.95.57:8080/Amazin_Spring_19_0/`
   - Autenticación de usuarios

2. **Catálogo de Libros**: `http://156.35.95.57:8080/Amazin_Spring_19_0/private/showBooks`
   - Visualización de libros disponibles
   - Añadir libros al carrito
   - Reservar libros

3. **Carrito de Compra**: `http://156.35.95.57:8080/Amazin_Spring_19_0/private/viewCart`
   - Ver contenido del carrito
   - Gestionar items (comprar/eliminar)
   - Realizar checkout

4. **Mis Reservas**: `http://156.35.95.57:8080/Amazin_Spring_19_0/private/myReservations`
   - Ver reservas activas
   - Completar compra de reservas
   - Cancelar reservas

### Requisitos del Sistema

- Navegador web moderno (Chrome, Firefox, Edge, Safari)
- JavaScript habilitado
- Conexión al servidor en el puerto 8080

---

## Requisito 1: Compra de Libros

### 1.1 Control de Stock en Base de Datos

#### Modelo de Datos
Se ha modificado la entidad `Book` para incluir un atributo de stock:

```java
@Entity
public class Book {
    @Id @GeneratedValue
    private int id;
    private String title;
    private String description;
    private String author;
    private double basePrice;
    private int stock;  // ← NUEVO ATRIBUTO
    // ...
}
```

**Persistencia:** El atributo `stock` se mapea automáticamente a la base de datos mediante JPA. La configuración de JPA está en `persistence.xml`:

```xml
<property name="jakarta.persistence.schema-generation.database.action" value="update" />
```

Esta propiedad permite que Hibernate actualice el esquema de la base de datos automáticamente al detectar el nuevo campo.

**Inicialización:** Según el requisito, cada libro debe tener 10 unidades disponibles al despliegue. Esto se gestiona a través de scripts de inicialización de base de datos o mediante la inserción manual de los datos con valores de stock = 10.

---

### 1.2 Carrito de la Compra

#### Modelo del Carrito
Se han creado dos clases principales para gestionar el carrito:

**Cart.java** - Contenedor principal del carrito:
```java
public class Cart implements Serializable {
    private List<CartItem> items;
    
    // Añade un item al carrito, incrementa cantidad si ya existe
    public void addItem(Book book, int quantity, boolean isReserved);
    
    // Busca un item por ID de libro y tipo (reserva o compra)
    public CartItem findItemByBookId(int bookId, boolean isReserved);
    
    // Elimina un item específico
    public void removeItem(int bookId, boolean isReserved);
    
    // Calcula el total del carrito
    public double getTotal();
    
    // Vacía el carrito
    public void clear();
}
```

**CartItem.java** - Representa un item individual en el carrito:
```java
public class CartItem implements Serializable {
    private Book book;
    private int quantity;
    private boolean isReserved;  // Marca si es reserva o compra normal
    
    // Calcula el subtotal considerando si es reserva (95%) o compra (100%)
    public double getSubtotal() {
        if (isReserved) {
            return book.getPrice() * quantity * 0.95; // 95% restante
        }
        return book.getPrice() * quantity;
    }
}
```

#### Capa de Presentación

**CartController.java** - Gestiona las peticiones HTTP relacionadas con el carrito:

- **`addToCart`**: Añade un libro al carrito
  - Valida stock disponible antes de añadir
  - Utiliza la sesión HTTP para almacenar el carrito
  - Redirige al catálogo con mensaje de éxito/error

- **`viewCart`**: Muestra el contenido del carrito
  - Sincroniza las reservas desde la base de datos
  - Calcula el total incluyendo reservas pendientes

- **`removeFromCart`**: Elimina un item del carrito
  - Distingue entre items normales y reservas
  - Si es reserva, cancela en BD y restaura stock

- **`clearCart`**: Vacía todo el carrito
  - Cancela todas las reservas asociadas
  - Restaura el stock de los libros reservados

- **`checkout`**: Procesa la compra completa
  - Valida stock en tiempo real
  - Reduce stock en BD para compras normales
  - Procesa las reservas (elimina de BD sin restaurar stock)

#### Capa de Negocio

**CartManager.java** - Implementa la lógica de negocio del carrito:

```java
@Override
public void addBookToCart(Cart cart, int bookId, int quantity) throws Exception {
    // 1. Obtener el libro con precio calculado
    Book book = bookManagerService.getBookById(bookId);
    
    // 2. Calcular cantidad total solicitada
    int totalRequested = quantity;
    CartItem existingItem = cart.findItemByBookId(bookId, false);
    if (existingItem != null) {
        totalRequested += existingItem.getQuantity();
    }
    
    // 3. Verificar stock disponible
    if (!bookManagerService.checkStockAvailability(bookId, totalRequested)) {
        throw new Exception("cart.notEnoughStock");
    }
    
    // 4. Añadir al carrito (no reduce stock hasta checkout)
    cart.addItem(book, quantity, false);
}
```

**Sincronización con Reservas:**
```java
@Override
public void synchronizeCartForUser(String username, Cart cart) throws Exception {
    // Obtener reservas del usuario desde BD
    List<Reservation> reservations = reservationManagerService.getReservations(username);
    
    // Eliminar todas las reservas del carrito
    cart.removeAllItems(true);
    
    // Agregar todas las reservas desde BD (la BD es la fuente de verdad)
    for (Reservation res : reservations) {
        cart.addItem(res.getBook(), res.getQuantity(), true);
    }
}
```

**Proceso de Checkout:**
```java
@Override
public boolean checkout(String username, Cart cart) throws Exception {
    // 1. Procesar reservas (eliminar de BD sin restaurar stock)
    reservationManagerService.processReservationsInCart(username, cart);
    
    // 2. Procesar compras normales
    for (CartItem item : cart.getItems()) {
        if (!item.isReserved()) {
            // Reducir stock con validación en tiempo real
            boolean reduced = bookManagerService.reduceStock(item.getBookId(), item.getQuantity());
            if (!reduced) {
                return false; // Stock insuficiente
            }
        }
    }
    
    return true;
}
```

---

### 1.3 Validación de Disponibilidad

#### Capa de Negocio
**BookManager.java** expone métodos para verificar y modificar stock:

```java
@Override
public boolean checkStockAvailability(int bookId, int requestedQuantity) throws Exception {
    return bookDataService.checkStockAvailability(bookId, requestedQuantity);
}

@Override
public boolean reduceStock(int bookId, int quantity) throws Exception {
    return bookDataService.reduceStock(bookId, quantity);
}

@Override
public boolean increaseStock(int bookId, int quantity) throws Exception {
    bookDataService.increaseBookStock(bookId, quantity);
    return true;
}
```

#### Capa de Persistencia
**BookDAO.java** - Implementa las operaciones atómicas de stock:

**Verificación de Stock:**
```java
@Override
public boolean checkStockAvailability(int bookId, int requestedQuantity) throws Exception {
    Dba dba = new Dba(true); // Solo lectura
    try {
        EntityManager em = dba.getActiveEm();
        Book book = em.find(Book.class, bookId);
        
        if (book != null) {
            boolean available = book.getStock() >= requestedQuantity;
            logger.debug("Stock check: Requested=" + requestedQuantity + 
                        ", Available=" + book.getStock());
            return available;
        }
        return false;
    } finally {
        dba.closeEm();
    }
}
```

**Reducción de Stock (con bloqueo pesimista):**
```java
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

**Incremento de Stock (restauración):**
```java
@Override
public void increaseBookStock(int bookId, int quantity) throws Exception {
    Dba dba = new Dba();
    try {
        EntityManager em = dba.getActiveEm();
        
        // Bloqueo pesimista para operación atómica
        Book book = em.find(Book.class, bookId, 
                           jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        
        if (book != null) {
            int newStock = book.getStock() + quantity;
            book.setStock(newStock);
            em.merge(book);
            logger.debug("Stock increased: New stock=" + newStock);
        }
    } finally {
        dba.closeEm();
    }
}
```


---

### 1.4 Actualización de Stock tras Checkout

La actualización del stock se realiza en dos momentos:

1. **Durante la reserva**: El stock se reduce inmediatamente al crear/actualizar una reserva
2. **Durante el checkout**: El stock se reduce para las compras normales

**Proceso de Checkout completo:**
```java
@Override
public boolean checkout(String username, Cart cart) throws Exception {
    // Procesar reservas (ya redujeron stock al crearse)
    reservationManagerService.processReservationsInCart(username, cart);
    
    // Procesar compras normales (reducir stock ahora)
    for (CartItem item : cart.getItems()) {
        if (!item.isReserved()) {
            boolean reduced = bookManagerService.reduceStock(
                item.getBookId(), item.getQuantity()
            );
            if (!reduced) {
                return false;
            }
        }
    }
    return true;
}
```

---

## Requisito 2: Sistema de Reservas

### 2.1 Modelo de Reservas

#### Entidad Reservation
Se ha creado una nueva entidad JPA para gestionar las reservas:

```java
@Entity
public class Reservation {
    @Id @GeneratedValue
    private int id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;
    
    private String username;
    private int quantity;
    
    // Calcula el 5% pagado en la reserva
    public double getReservationPrice() {
        return book.getPrice() * quantity * 0.05;
    }
    
    // Calcula el 95% restante por pagar
    public double getRemainingAmount() {
        return book.getPrice() * quantity * 0.95;
    }
}
```

**Relación con Book:** Se establece una relación `@ManyToOne` con carga eager para que siempre se cargue el libro junto con la reserva, evitando problemas de lazy loading.

---

### 2.2 Funcionalidad de Reserva

#### Capa de Presentación

**ReservationController.java** - Gestiona las peticiones de reservas:

```java
@RequestMapping("private/reserveBook")
public String reserveBook(@RequestParam("bookId") int bookId, 
                         @RequestParam("quantity") int quantity,
                         Principal principal, HttpSession session) {
    
    synchronized (servletContext) {
        try {
            String username = principal.getName();
            
            // Toda la lógica encapsulada en el servicio
            reservationManagerService.reserveBook(username, bookId, quantity);
            
            session.setAttribute("message", "reservation.created");
            return "redirect:showBooks";
            
        } catch (Exception e) {
            handleReservationError(e, session, null, "Error reserving book", true);
            return "redirect:showBooks";
        }
    }
}
```

#### Capa de Negocio

**ReservationManager.java** - Lógica de negocio de reservas:

```java
@Override
public Reservation reserveBook(String username, int bookId, int quantity) throws Exception {
    // 1. Verificar si ya existe una reserva del usuario para este libro
    Reservation existingReservation = getReservationByUserAndBook(username, bookId);
    boolean isNewReservation = (existingReservation == null);
    
    // 2. Si es nueva, validar que el libro existe
    if (isNewReservation) {
        Book book = bookManagerService.getBookById(bookId);
        if (book == null) {
            throw new Exception("reservation.bookNotFound");
        }
    }
    
    // 3. Verificar y reducir stock (IMPORTANTE: reduce inmediatamente)
    if (!bookManagerService.checkStockAvailability(bookId, quantity)) {
        throw new Exception("reservation.notEnoughStock");
    }
    
    boolean stockReduced = bookManagerService.reduceStock(bookId, quantity);
    if (!stockReduced) {
        throw new Exception("reservation.stockReductionFailed");
    }
    
    try {
        Reservation reservation;
        
        if (isNewReservation) {
            // Crear nueva reserva
            reservation = reservationDataService.createReservation(
                username, bookId, quantity
            );
        } else {
            // Actualizar reserva existente (incrementar cantidad)
            int newQuantity = existingReservation.getQuantity() + quantity;
            existingReservation.setQuantity(newQuantity);
            reservation = reservationDataService.updateReservation(existingReservation);
        }
        
        return reservation;
        
    } catch (Exception e) {
        // Rollback: restaurar stock si falla la creación/actualización
        bookManagerService.increaseStock(bookId, quantity);
        throw e;
    }
}
```

**Características importantes:**
- **Reducción inmediata de stock**: Al crear una reserva, el stock se reduce inmediatamente para que no esté disponible para otros usuarios
- **Reservas acumulativas**: Si un usuario reserva el mismo libro varias veces, se incrementa la cantidad en lugar de crear múltiples reservas
- **Rollback automático**: Si falla la creación de la reserva en BD, se restaura el stock automáticamente

---

### 2.3 Integración con el Carrito

#### Marca Especial en el Carrito
Los items reservados se distinguen en el carrito mediante el atributo `isReserved`:

```java
public class CartItem {
    private boolean isReserved;
    
    // El subtotal se calcula según el tipo
    public double getSubtotal() {
        if (isReserved) {
            return book.getPrice() * quantity * 0.95; // 95% pendiente
        }
        return book.getPrice() * quantity; // 100% precio completo
    }
}
```

#### Vista del Carrito (viewCart.jsp)
```jsp
<td>
    <c:choose>
        <c:when test="${item.reserved}">
            <span style="color: orange; font-weight: bold;">
                <spring:message code="cart.reservation"/> 
                (95% <spring:message code="cart.pending"/>)
            </span>
        </c:when>
        <c:otherwise>
            <spring:message code="cart.purchase"/>
        </c:otherwise>
    </c:choose>
</td>
```

#### Sincronización Automática
Cada vez que un usuario accede a `viewCart`, el sistema sincroniza automáticamente las reservas desde la base de datos:

```java
@RequestMapping("private/viewCart")
public String viewCart(Principal principal, HttpSession session, Model model) {
    String username = principal.getName();
    Cart cart = getOrCreateCart(session);
    
    // Sincronizar con reservas (la BD es la fuente de verdad)
    cartManagerService.synchronizeCartForUser(username, cart);
    
    session.setAttribute(CART_ATTRIBUTE, cart);
    model.addAttribute("total", cart.getTotal());
    return "private/viewCart";
}
```

---

### 2.4 Reducción de Stock en Reservas

**Momento de reducción:** El stock se reduce **inmediatamente** al crear o incrementar una reserva, no al finalizar la compra. Esto garantiza que los libros reservados no estén disponibles para otros usuarios.

**Implementación en ReservationManager:**
```java
// Verificar y reducir stock (común para ambos casos)
if (!bookManagerService.checkStockAvailability(bookId, quantity)) {
    throw new Exception("reservation.notEnoughStock");
}

boolean stockReduced = bookManagerService.reduceStock(bookId, quantity);
if (!stockReduced) {
    throw new Exception("reservation.stockReductionFailed");
}
```

**Consecuencias:**
- Los libros reservados se muestran con stock reducido en el catálogo
- Otros usuarios no pueden comprar ni reservar libros que ya están reservados
- Si se cancela una reserva, el stock se restaura inmediatamente

---

### 2.5 Sección "Mis Reservas"

#### Controlador
```java
@RequestMapping("private/myReservations")
public String myReservations(Principal principal, Model model) {
    try {
        String username = principal.getName();
        List<Reservation> reservations = 
            reservationManagerService.getReservations(username);
        
        model.addAttribute("reservations", reservations);
        return "private/myReservations";
        
    } catch (Exception e) {
        logger.error("Error getting reservations", e);
        model.addAttribute("error", "error.general");
        return "private/error";
    }
}
```

#### Vista (myReservations.jsp)
La vista muestra una tabla con:
- Título del libro
- Autor
- Precio unitario
- Cantidad reservada
- Importe pagado (5%)
- Importe pendiente (95%)
- Botones "Comprar" y "Eliminar"

```jsp
<c:forEach var='reservation' items="${reservations}">
    <tr>
        <td><c:out value="${reservation.book.title}" /></td>
        <td><c:out value="${reservation.book.author}" /></td>
        <td><c:out value="${reservation.book.price}" /> €</td>
        <td><c:out value="${reservation.quantity}" /></td>
        <td><c:out value="${reservation.reservationPrice}" /> €</td>
        <td><c:out value="${reservation.remainingAmount}" /> €</td>
        <td>
            <form action="purchaseReservation" method="post">
                <input type="hidden" name="reservationId" value="${reservation.id}"/>
                <input type="submit" value="Comprar"/>
            </form>
            <form action="cancelReservationFromPage" method="post">
                <input type="hidden" name="reservationId" value="${reservation.id}"/>
                <input type="submit" value="Eliminar"/>
            </form>
        </td>
    </tr>
</c:forEach>
```

---

### 2.6 Compra de Reserva (Pago del 95%)

#### Controlador
```java
@RequestMapping("private/purchaseReservation")
public String purchaseReservation(@RequestParam("reservationId") int reservationId,
                                 Principal principal, Model model) {
    synchronized (servletContext) {
        try {
            String username = principal.getName();
            
            // El servicio valida internamente la propiedad
            reservationManagerService.purchaseReservation(reservationId, username);
            
            model.addAttribute("message", "reservation.purchased");
            return "redirect:myReservations";
            
        } catch (Exception e) {
            handleReservationError(e, null, model, "Error purchasing reservation", false);
            return "redirect:myReservations";
        }
    }
}
```

#### Lógica de Negocio
```java
@Override
public boolean purchaseReservation(int reservationId, String username) throws Exception {
    // Obtener y validar propiedad
    Reservation reservation = getReservationById(reservationId, username);
    
    // Procesar compra: eliminar reserva de BD
    // El stock NO se restaura porque ya se había reducido al crear la reserva
    reservationDataService.deleteReservation(reservationId);
    
    logger.debug("Reservation purchased. Stock remains reduced.");
    return true;
}
```

**Validación de propiedad:**
```java
private Reservation getReservationById(int reservationId, String username) throws Exception {
    Reservation reservation = reservationDataService.getReservationById(reservationId);
    
    if (reservation == null) {
        throw new Exception("reservation.notFound");
    }
    
    if (!reservation.getUsername().equals(username)) {
        logger.warn("User {} tried to access reservation {} belonging to {}", 
                   username, reservationId, reservation.getUsername());
        throw new Exception("reservation.accessDenied");
    }
    
    return reservation;
}
```

---

### 2.7 Eliminación de Reserva (Restauración de Stock)

#### Controlador
```java
@RequestMapping("private/cancelReservationFromPage")
public String cancelReservationFromPage(@RequestParam("reservationId") int reservationId,
                                       Principal principal, Model model) {
    synchronized (servletContext) {
        try {
            String username = principal.getName();
            
            // El servicio valida internamente la propiedad
            reservationManagerService.cancelReservation(reservationId, username);
            
            model.addAttribute("message", "reservation.cancelled");
            return "redirect:myReservations";
            
        } catch (Exception e) {
            handleReservationError(e, null, model, "Error cancelling reservation", false);
            return "redirect:myReservations";
        }
    }
}
```

#### Lógica de Negocio
```java
@Override
public boolean cancelReservation(int reservationId, String username) throws Exception {
    // Obtener y validar propiedad
    Reservation reservation = getReservationById(reservationId, username);
    
    // Restaurar stock (IMPORTANTE: devuelve las unidades al stock)
    Book book = reservation.getBook();
    bookManagerService.increaseStock(book.getId(), reservation.getQuantity());
    
    // Eliminar reserva
    reservationDataService.deleteReservation(reservationId);
    
    logger.debug("Reservation cancelled and stock restored");
    return true;
}
```

**Flujo completo:**
1. Se valida que la reserva pertenece al usuario autenticado
2. Se incrementa el stock del libro en la cantidad reservada
3. Se elimina la reserva de la base de datos
4. Las unidades vuelven a estar disponibles para compra/reserva

---

### 2.8 Capa de Persistencia - ReservationDAO

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
        
        em.persist(reservation);
        return reservation;
    } finally {
        dba.closeEm();
    }
}

@Override
public List<Reservation> getReservationsByUsername(String username) throws Exception {
    Dba dba = new Dba(true); // Solo lectura
    try {
        EntityManager em = dba.getActiveEm();
        
        TypedQuery<Reservation> query = em.createQuery(
            "SELECT r FROM Reservation r WHERE r.username = :username ORDER BY r.id DESC", 
            Reservation.class
        );
        query.setParameter("username", username);
        
        List<Reservation> results = query.getResultList();
        
        // Forzar carga de libros para evitar lazy loading issues
        for (Reservation r : results) {
            r.getBook().getTitle();
        }
        
        return results;
    } finally {
        dba.closeEm();
    }
}

@Override
public Reservation updateReservation(Reservation reservation) throws Exception {
    Dba dba = new Dba();
    try {
        EntityManager em = dba.getActiveEm();
        
        // Obtener la reserva gestionada por este EntityManager
        Reservation managed = em.find(Reservation.class, reservation.getId());
        if (managed == null) {
            throw new Exception("Reservation not found");
        }
        
        // Actualizar solo el campo quantity
        managed.setQuantity(reservation.getQuantity());
        
        return managed;
    } finally {
        dba.closeEm();
    }
}

@Override
public void deleteReservation(int id) throws Exception {
    Dba dba = new Dba();
    try {
        EntityManager em = dba.getActiveEm();
        Reservation reservation = em.find(Reservation.class, id);
        if (reservation != null) {
            em.remove(reservation);
        }
    } finally {
        dba.closeEm();
    }
}
```

---

## Requisito 3: Internacionalización

### 3.1 Configuración de Spring

#### Definición del LocaleResolver
En `servlet-context.xml`:

```xml
<!-- Configuración de LocaleResolver para gestión de idiomas -->
<bean id="localeResolver" class="org.springframework.web.servlet.i18n.SessionLocaleResolver">
    <property name="defaultLocale" value="es" />
</bean>
```

**SessionLocaleResolver:** Almacena el idioma seleccionado en la sesión HTTP del usuario, manteniendo la preferencia durante toda la sesión.

#### Definición del MessageSource
En `beans.xml`:

```xml
<bean id="messageSource"
      class="org.springframework.context.support.ResourceBundleMessageSource">
    <property name="basename" value="messages" />
    <property name="defaultEncoding" value="UTF-8" />
    <property name="fallbackToSystemLocale" value="false" />
</bean>
```

**Propiedades:**
- `basename`: Nombre base de los archivos de propiedades (messages.properties, messages_en.properties)
- `defaultEncoding`: UTF-8 para soportar caracteres especiales en español
- `fallbackToSystemLocale`: false para usar siempre el idioma configurado, no el del sistema

---

### 3.2 Archivos de Recursos

#### messages.properties (Español - por defecto)
```properties
welcome=Bienvenido a <em>la más pequeña</em> tienda virtual del mundo!
login.title=Introduzca usuario y contraseña
book.title=Título
cart.addToCart=Añadir al carrito
cart.viewCart=Ver carrito
cart.shoppingCart=Carrito de compra
reservation.reserve=Reservar
reservation.myReservations=Mis Reservas
language.select=Idioma
language.spanish=Español
language.english=Inglés
# ... más de 100 claves definidas
```

#### messages_en.properties (Inglés)
```properties
welcome=Welcome to the <em>smallest</em> virtual shop in the world!!!
login.title=Introduce login and password
book.title=Title
cart.addToCart=Add to Cart
cart.viewCart=View Cart
cart.shoppingCart=Shopping Cart
reservation.reserve=Reserve
reservation.myReservations=My Reservations
language.select=Language
language.spanish=Spanish
language.english=English
# ... más de 100 claves definidas
```

**Cobertura completa:** Se han internacionalizado todos los textos de la aplicación:
- Formularios de login
- Catálogo de libros
- Carrito de compra
- Sistema de reservas
- Mensajes de error y éxito
- Navegación y menús
- Footer y headers

---

### 3.3 Selector de Idioma

#### Componente Reutilizable (languageSelector.jsp)
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
- **Sin botones**: El cambio se realiza automáticamente al seleccionar un valor mediante el evento `onchange`
- **Selección persistente**: El selector muestra el idioma actualmente activo
- **Componente reutilizable**: Se incluye en todas las páginas mediante `<jsp:include>`

---

### 3.4 Controlador de Cambio de Idioma

**LanguageController.java:**
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
        
        // Establecer el nuevo locale en la sesión
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

**Flujo de funcionamiento:**
1. El usuario selecciona un idioma en el desplegable
2. JavaScript captura el evento `onchange` y hace una petición GET a `/changeLanguage?lang=XX`
3. El controlador establece el nuevo locale en la sesión
4. Se redirige al usuario a la misma página donde estaba (usando el header Referer)
5. La página se recarga mostrando todos los textos en el nuevo idioma

---

### 3.5 Configuración de Seguridad

En `SecurityConfig.java` se permite el acceso sin autenticación al endpoint de cambio de idioma:

```java
.requestMatchers("/resources/**", "/changeLanguage").permitAll()
```

Esto permite que los usuarios cambien el idioma incluso en la página de login.

---

### 3.6 Uso en las Vistas JSP

**Importación de librerías:**
```jsp
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring"%>
```

**Uso de mensajes internacionalizados:**
```jsp
<!-- Texto simple -->
<h2><spring:message code="cart.shoppingCart"/></h2>

<!-- En atributos de formularios -->
<input type="submit" value="<spring:message code='cart.addToCart'/>" />

<!-- En tablas -->
<th><spring:message code="cart.title"/></th>
<th><spring:message code="cart.quantity"/></th>

<!-- Mensajes dinámicos desde el modelo -->
<spring:message code="${sessionScope.message}"/>
```

**Inclusión del selector:**
```jsp
<!-- En todas las páginas -->
<jsp:include page="../languageSelector.jsp" />
```

---

## Arquitectura General

### Patrón de Capas

La aplicación sigue una arquitectura en 3 capas bien definida:

```
PRESENTACIÓN (Controllers)
    ↓
NEGOCIO (Managers/Services)
    ↓
PERSISTENCIA (DAOs)
    ↓
BASE DE DATOS
```

### Diagrama de Componentes

```
┌─────────────────────────────────────────────────────────┐
│                   CAPA DE PRESENTACIÓN                   │
│  CartController  │ ReservationController │ LanguageController│
└──────────────┬───────────────┬──────────────────────────┘
               │               │
┌──────────────▼───────────────▼──────────────────────────┐
│                    CAPA DE NEGOCIO                       │
│  CartManager  │ ReservationManager │ BookManager         │
└──────────────┬───────────────┬──────────────────────────┘
               │               │
┌──────────────▼───────────────▼──────────────────────────┐
│                  CAPA DE PERSISTENCIA                    │
│      BookDAO      │    ReservationDAO    │   VATDAO       │
└──────────────┬───────────────┬──────────────────────────┘
               │               │
               ▼               ▼
         ┌────────────────────────┐
         │   BASE DE DATOS HSQLDB │
         └────────────────────────┘
```

---

### Responsabilidades por Capa

#### Capa de Presentación
- **Responsabilidad**: Gestionar peticiones HTTP, validación básica, control de sesión
- **Componentes**: Controllers (CartController, ReservationController, LanguageController)
- **No debe**: Contener lógica de negocio, acceder directamente a la BD

#### Capa de Negocio
- **Responsabilidad**: Lógica de negocio, validaciones complejas, coordinación entre DAOs
- **Componentes**: Managers/Services (CartManager, ReservationManager, BookManager)
- **No debe**: Gestionar peticiones HTTP, conocer detalles de implementación de persistencia

#### Capa de Persistencia
- **Responsabilidad**: Acceso a datos, operaciones CRUD, gestión de transacciones
- **Componentes**: DAOs (BookDAO, ReservationDAO, VATDAO)
- **No debe**: Contener lógica de negocio, gestionar sesiones HTTP

---

### Inyección de Dependencias

Todas las dependencias se gestionan mediante **Spring IoC** con configuración XML:

```xml
<!-- beans.xml -->
<bean id="cartManagerService" class="com.miw.business.cartmanager.CartManager"/>
<bean id="reservationManagerService" class="com.miw.business.reservationmanager.ReservationManager"/>
<bean id="bookDataService" class="com.miw.persistence.book.BookDAO"/>
<bean id="reservationDataService" class="com.miw.persistence.reservation.ReservationDAO"/>
```

**Inyección en Controladores:**
```java
@Controller
public class CartController {
    @Autowired
    private CartManagerService cartManagerService;
}
```

**Inyección en Managers:**
```java
public class CartManager implements CartManagerService {
    @Autowired
    private BookManagerService bookManagerService;
    
    @Autowired
    private ReservationManagerService reservationManagerService;
}
```

---

## Consideraciones Técnicas

### 5.1 Concurrencia y Sincronización

#### Bloqueos Pesimistas en Base de Datos
Para evitar condiciones de carrera en operaciones de stock:

```java
Book book = em.find(Book.class, bookId, 
                   jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
```

**Ventajas:**
- Garantiza atomicidad de operaciones
- Previene lecturas sucias
- Evita actualizaciones perdidas

#### Sincronización con ServletContext
Para operaciones críticas que requieren serialización:

```java
@RequestMapping("private/reserveBook")
public String reserveBook(...) {
    synchronized (servletContext) {
        // Operación crítica
    }
}
```

---

### 5.2 Gestión de Sesiones

#### Almacenamiento del Carrito
El carrito se almacena en la sesión HTTP:

```java
HttpSession session = ...;
Cart cart = (Cart) session.getAttribute("cart");
if (cart == null) {
    cart = new Cart();
    session.setAttribute("cart", cart);
}
```

#### Sincronización de Reservas
Las reservas se almacenan en BD y se sincronizan con el carrito en cada vista:

```java
cartManagerService.synchronizeCartForUser(username, cart);
```

**Ventajas:**
- La BD es la fuente de verdad
- Consistencia entre sesiones
- Resistencia a pérdida de sesión

---

### 5.3 Manejo de Errores

#### Excepciones con Claves de Internacionalización
```java
throw new Exception("cart.notEnoughStock");
throw new Exception("reservation.notFound");
throw new Exception("reservation.accessDenied");
```

#### Manejo Centralizado en Controladores
```java
private void handleCartError(Exception e, HttpSession session, Model model, 
                            String logMessage, boolean useSession) {
    logger.error(logMessage, e);
    
    String errorKey = "error.general";
    if (errorMsg != null && errorMsg.startsWith("cart.")) {
        errorKey = errorMsg;
    }
    
    if (useSession) {
        session.setAttribute("error", errorKey);
    } else {
        model.addAttribute("error", errorKey);
    }
}
```

#### Visualización en JSP
```jsp
<c:if test="${not empty sessionScope.error}">
    <div style="background-color: #f8d7da; ...">
        <spring:message code="${sessionScope.error}"/>
    </div>
    <c:remove var="error" scope="session"/>
</c:if>
```

---

### 5.4 Transacciones

#### Gestión con JPA
Todas las operaciones de escritura se realizan dentro de transacciones:

```java
Dba dba = new Dba(); // Inicia transacción
try {
    EntityManager em = dba.getActiveEm();
    // Operaciones de BD
    // Commit automático al cerrar
} catch (Exception e) {
    // Rollback automático en caso de error
    throw e;
} finally {
    dba.closeEm(); // Cierra EM y confirma/revierte transacción
}
```

#### Operaciones de Solo Lectura
```java
Dba dba = new Dba(true); // Solo lectura, sin transacción
```

---

### 5.5 Seguridad

#### Autenticación y Autorización
Implementado con **Spring Security**:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
            .requestMatchers("/resources/**", "/changeLanguage").permitAll()
            .requestMatchers("/private/**").authenticated()
        );
        return http.build();
    }
}
```

#### Validación de Propiedad
En operaciones de reservas:

```java
if (!reservation.getUsername().equals(username)) {
    throw new Exception("reservation.accessDenied");
}
```

#### Protección CSRF
Tokens CSRF en todos los formularios:

```jsp
<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
```

---

### 5.6 Logging

#### Configuración Log4j2
Logging extensivo en todas las capas:

```java
Logger logger = LogManager.getLogger(this.getClass());

logger.debug("Adding book {} to cart. Quantity: {}", bookId, quantity);
logger.error("Error processing checkout", e);
logger.warn("User {} tried to access reservation {}", username, reservationId);
```

**Niveles utilizados:**
- **DEBUG**: Flujo normal de ejecución, valores de variables
- **ERROR**: Excepciones y errores críticos
- **WARN**: Situaciones anómalas no críticas

---

### 5.7 Validaciones

#### Validación de Stock
```java
if (!bookManagerService.checkStockAvailability(bookId, requestedQuantity)) {
    throw new Exception("cart.notEnoughStock");
}
```

#### Validación de Entidades
```java
if (book == null) {
    throw new Exception("cart.bookNotFound");
}

if (reservation == null) {
    throw new Exception("reservation.notFound");
}
```

#### Validación en la Vista
```jsp
<c:if test="${book.stock > 0}">
    <!-- Formulario de compra -->
</c:if>
<c:if test="${book.stock <= 0}">
    <span style="color: red;">Sin stock</span>
</c:if>
```

---

## Conclusiones

### Funcionalidades Implementadas

✅ **Requisito 1: Compra de Libros**
- Control de stock en base de datos
- Carrito de compra funcional
- Validación de disponibilidad en tiempo real
- Actualización de stock tras checkout
- Al menos 3 libros con 10 unidades iniciales cada uno

✅ **Requisito 2: Sistema de Reservas**
- Funcionalidad de reservar libros
- Marca especial en carrito (5% pagado, 95% pendiente)
- Reducción de stock inmediata al reservar
- Sección "Mis Reservas" con opciones Comprar/Eliminar
- Pago del 95% restante al comprar
- Restauración de stock al eliminar reserva

✅ **Requisito 3: Internacionalización**
- Soporte completo para español (es) e inglés (en)
- Selector de idioma en todas las páginas
- Cambio automático sin botones (evento onchange)
- Más de 100 claves de traducción
- Persistencia del idioma en sesión

### Arquitectura Respetada

- ✅ Separación en 3 capas (Presentación, Negocio, Persistencia)
- ✅ Inyección de dependencias con Spring
- ✅ Convenios de nombres del piloto original
- ✅ Uso de interfaces para servicios
- ✅ Gestión de transacciones con JPA
- ✅ Configuración declarativa en XML

### Calidad del Código

- ✅ Logging extensivo en todas las capas
- ✅ Manejo centralizado de excepciones
- ✅ Validaciones en múltiples niveles
- ✅ Código documentado y comentado
- ✅ Métodos auxiliares privados para código limpio
- ✅ Principio de responsabilidad única

### Aspectos Técnicos Destacables

- **Bloqueos pesimistas** en operaciones de stock para garantizar consistencia
- **Sincronización con BD** en cada acceso al carrito
- **Rollback automático** si falla la creación de reservas
- **Validación de propiedad** en operaciones de reservas
- **Mensajes internacionalizados** incluso en excepciones
- **Protección CSRF** en todos los formularios

---

## Anexo: Estructura de Archivos

```
src/main/java/com/miw/
├── business/
│   ├── bookmanager/
│   │   ├── BookManager.java
│   │   ├── BookManagerService.java
│   │   └── BookManagerEJBService.java
│   ├── cartmanager/
│   │   ├── CartManager.java (NUEVO)
│   │   └── CartManagerService.java (NUEVO)
│   ├── reservationmanager/
│   │   ├── ReservationManager.java (NUEVO)
│   │   └── ReservationManagerService.java (NUEVO)
│   └── ServicesFactory.java
├── config/
│   └── SecurityConfig.java
├── model/
│   ├── Book.java (MODIFICADO - stock)
│   ├── Cart.java (NUEVO)
│   ├── CartItem.java (NUEVO)
│   ├── Reservation.java (NUEVO)
│   ├── VAT.java
│   └── LoginData.java
├── persistence/
│   ├── book/
│   │   ├── BookDAO.java (MODIFICADO - stock methods)
│   │   └── BookDataService.java (MODIFICADO)
│   ├── reservation/
│   │   ├── ReservationDAO.java (NUEVO)
│   │   └── ReservationDataService.java (NUEVO)
│   ├── vat/
│   │   ├── VATDAO.java
│   │   └── VATDataService.java
│   └── Dba.java
└── presentation/
    ├── CartController.java (NUEVO)
    ├── ReservationController.java (NUEVO)
    ├── LanguageController.java (NUEVO)
    ├── BookController.java
    └── ...

src/main/resources/
├── messages.properties (NUEVO - español)
├── messages_en.properties (NUEVO - inglés)
├── log4j.properties
└── META-INF/
    └── persistence.xml (MODIFICADO)

src/main/webapp/WEB-INF/
├── views/
│   ├── languageSelector.jsp (NUEVO)
│   ├── login.jsp (MODIFICADO)
│   └── private/
│       ├── showBooks.jsp (MODIFICADO)
│       ├── viewCart.jsp (NUEVO)
│       ├── myReservations.jsp (NUEVO)
│       ├── checkoutSuccess.jsp (NUEVO)
│       └── ...
└── spring/
    ├── appServlet/
    │   ├── servlet-context.xml (MODIFICADO - locale)
    │   └── beans.xml (MODIFICADO - nuevos beans)
    └── root-context.xml
```

---

## Información de Despliegue Final

### Acceso a la Aplicación Desplegada

La aplicación ha sido desplegada en un servidor Tomcat y está disponible en:

**URL:** http://156.35.95.57:8080/Amazin_Spring_19_0/

### Verificación de Funcionalidades

Para verificar que todas las funcionalidades están operativas:

1. **Sistema de Compra de Libros**:
   - Acceder al catálogo: http://156.35.95.57:8080/Amazin_Spring_19_0/private/showBooks
   - Añadir libros al carrito
   - Verificar control de stock (no se puede añadir más unidades de las disponibles)
   - Realizar checkout
   - Comprobar que el stock se ha actualizado en la base de datos

2. **Sistema de Reservas**:
   - Reservar un libro desde el catálogo
   - Acceder a "Mis Reservas": http://156.35.95.57:8080/Amazin_Spring_19_0/private/myReservations
   - Verificar que aparece el 5% pagado y el 95% pendiente
   - Comprobar integración con el carrito
   - Probar las opciones "Comprar" y "Eliminar"

3. **Internacionalización**:
   - Cambiar el idioma usando el selector en cualquier página
   - Verificar que todos los textos se actualizan (español/inglés)
   - Comprobar que el idioma persiste durante la navegación

### Datos de Prueba

- **Stock inicial**: Cada libro tiene 10 unidades
- **Número mínimo de libros**: 3 libros diferentes en el catálogo
- **Usuarios**: Configurados según el sistema de autenticación de Spring Security

---

**Documento generado el:** 27 de octubre de 2025  
**Autor:** Implementación del Trabajo 2 - MIW  
**Versión:** 1.1  
**URL de Despliegue:** http://156.35.95.57:8080/Amazin_Spring_19_0/

