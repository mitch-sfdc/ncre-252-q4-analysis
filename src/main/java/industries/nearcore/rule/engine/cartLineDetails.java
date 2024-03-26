package industries.nearcore.rule.engine;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class cartLineDetails implements Serializable{
    private static final long serialVersionUID = 1L;
    private String cartLineItemId;
    private String cartLineProductCategoryId;
    private Double cartLineItemQuantity;
    private String cartLineProductId;
    private Map<String, Double> promotions = new HashMap<>();


    public String getCartLineItemId(){
        return cartLineItemId;
    }

    public void setCartLineItemId( String cartLineItemId ){
        this.cartLineItemId = cartLineItemId;
    }

    public String getCartLineProductCategoryId(){
        return cartLineProductCategoryId;
    }

    public void setCartLineProductCategoryId( String cartLineProductCategoryId ){
        this.cartLineProductCategoryId = cartLineProductCategoryId;
    }

    public Double getCartLineItemQuantity(){
        return cartLineItemQuantity;
    }

    public void setCartLineItemQuantity( Double cartLineItemQuantity ){
        this.cartLineItemQuantity = cartLineItemQuantity;
    }

    public String getCartLineProductId(){
        return cartLineProductId;
    }

    public void setCartLineProductId( String cartLineProductId ){
        this.cartLineProductId = cartLineProductId;
    }

    public Map<String, Double> getPromotions() {
        return promotions;
    }

    public void setPromotions(Map<String, Double> promotions) {
        this.promotions = promotions;
    }

    public void addPromotion(String ruleName, Double discountAmount) {
        this.promotions.put(ruleName, discountAmount);
    }

    public String toJson() {
        StringBuilder builder = new StringBuilder("{");

        // add the attributes
        builder.append("    ")
                .append("\"cartLineItemId\" : \"").append(this.cartLineItemId).append("\", ")
                .append("\"cartLineProductCategoryId\" : \"").append(this.cartLineProductCategoryId).append("\", ")
                .append("\"cartLineItemQuantity\" : ").append(this.cartLineItemQuantity).append(", ")
                .append("\"cartLineProductId\" : \"").append(this.cartLineProductId).append("\", ")
                .append("\"promotions\" : [");

        // add each discount
        boolean firstLine = true;
        for (Map.Entry entry : this.promotions.entrySet()) {
            if(firstLine) {
                firstLine = false;
            } else {
                builder.append(",\n");
            }
            builder.append("    ").append("\"").append(entry.getKey()).append(" : ").append(entry.getValue());
        }

        // close off
        builder.append("    ")
                .append("]\n")
                .append("}");

        return builder.toString();
    }
  }