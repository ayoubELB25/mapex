package dev.ayoubelb25.mapex.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Entity
@Table(name = "transaction")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "ISIN is required")
    @Pattern(regexp = "^MA\\d{10}$", message = "ISIN must be MA + 10 digits")
    @Column(name = "isin", nullable = false, length = 12)
    private String isin;

    @NotBlank(message = "Side is required")
    @Column(name = "side", nullable = false, length = 5)
    private String side;
    
    @Positive(message = "Quantity must be positive")
    @Column(name = "quantity", nullable = false)
    private int quantity;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than zero")
    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @NotBlank(message = "Order type is required")
    @Column(name = "order_type", nullable = false, length = 10)
    private String orderType;

    @Column(name = "status", length = 20)
    private String status = "PENDING";

    // ── Getters and setters ──────────────────────────────────────────
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIsin() {
        return isin;
    }

    public void setIsin(String isin) {
        this.isin = isin;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String o) {
        this.orderType = o;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
