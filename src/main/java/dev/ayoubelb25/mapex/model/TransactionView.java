package dev.ayoubelb25.mapex.model;

import java.math.BigDecimal;


public class TransactionView {

    private Long       id;
    private String     isin;
    private String     side;        
    private Integer    quantity;
    private BigDecimal price;
    private String     orderType;   
    private String     status;      

    public TransactionView(Long id, String isin, String side,
                           Integer quantity, BigDecimal price,
                           String orderType, String status) {
        this.id        = id;
        this.isin      = isin;
        this.side      = side;
        this.quantity  = quantity;
        this.price     = price;
        this.orderType = orderType;
        this.status    = status;
    }

    public Long       getId()        { return id; }
    public String     getIsin()      { return isin; }
    public String     getSide()      { return side; }
    public Integer    getQuantity()  { return quantity; }
    public BigDecimal getPrice()     { return price; }
    public String     getOrderType() { return orderType; }
    public String     getStatus()    { return status; }
}
