package industries.nearcore.rule.engine;

import java.util.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;


public class cartDetails implements Serializable{
    private static final long serialVersionUID = 1L;
    private List<cartLineDetails> cartLineDetailsList;
    private Double transactionAmount;
    private String currencyISOCode;
    private Double discountAmount = 0.0;
    private loyaltyMember loyaltyMember = null;
    private Map<String, Double> promotions = new HashMap<>();
    private Set<String> qualifiedPromotions = new HashSet<>();


    public List<cartLineDetails> getCartLineDetailsList(){
        return cartLineDetailsList;
    }

    public void setCartLineDetailsList( List<cartLineDetails> cartLineDetailsList ){
        this.cartLineDetailsList = cartLineDetailsList;
    }

    public Double getTransactionAmount(){
        return transactionAmount;
    }

    public void setTransactionAmount( Double transactionAmount ){
        this.transactionAmount = transactionAmount;
    }

    public String getCurrencyISOCode(){
        return currencyISOCode;
    }

    public void setCurrencyISOCode( String currencyISOCode ){
        this.currencyISOCode = currencyISOCode;
    }

    public Double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public industries.nearcore.rule.engine.loyaltyMember getLoyaltyMember() {
        return loyaltyMember;
    }

    public void setLoyaltyMember(industries.nearcore.rule.engine.loyaltyMember loyaltyMember) {
        this.loyaltyMember = loyaltyMember;
    }

    public Map<String, Double> getPromotions() {
        return promotions;
    }

    public void addPromotion(String ruleName, Double promotionAmount) {
        this.promotions.put(ruleName, promotionAmount);
    }

    public void setPromotions(Map<String, Double> promotions) {
        this.promotions = promotions;
    }

    public Set<String> getQualifiedPromotions() {
        return qualifiedPromotions;
    }

    public void setQualifiedPromotions(Set<String> qualifiedPromotions) {
        this.qualifiedPromotions = qualifiedPromotions;
    }

    public void addQualifiedPromotion(String ruleName) { this.qualifiedPromotions.add(ruleName); }

    // generate JSON
    public String toJson() {
        StringBuilder builder = new StringBuilder("{\n");

        // add the attributes
        builder .append("    \"").append(this.getClass().getSimpleName()).append("\" : {\n")
                .append("        ").append("\"transactionAmount\" : ").append(this.transactionAmount).append(",\n")
                .append("        ").append("\"currencyISOCode\" : \"").append(this.currencyISOCode).append("\",\n")
                .append("        ").append("\"discountAmount\" : ").append(this.discountAmount).append(",\n");

        // add the loyalty membership
        builder.append("        ").append("\"loyaltyMember\" : {\n");
        if(loyaltyMember != null) {
            builder.append("            \"isLoyaltyMember\" : ")
                    .append(loyaltyMember.getIsLoyaltyMember())
                    .append("\n");
        }
        builder.append("        },\n");

        // add the line items
        builder.append("        ").append("\"cartLineDetailsList\" : [\n");
        boolean firstLine = true;
        for (cartLineDetails lineItem : this.cartLineDetailsList) {
            if(!firstLine) {
                builder.append(",\n");
            } else {
                firstLine = false;
            }
            builder.append("            ").append(lineItem.toJson());
        }
        builder.append("\n        ]\n").append("    }\n").append("}");

        return builder.toString();
    }
}