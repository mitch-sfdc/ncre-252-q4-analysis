package industries.nearcore.rule.engine;

import java.util.List;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Currency;
import java.time.LocalDateTime;


public class loyaltyMember implements Serializable{
    private static final long serialVersionUID = 1L;
    private List<loyaltyMemberTier> loyaltyMemberTierList;
    private Boolean isLoyaltyMember;


    public List<loyaltyMemberTier> getLoyaltyMemberTierList(){
        return loyaltyMemberTierList;
    }

    public void setLoyaltyMemberTierList( List<loyaltyMemberTier> loyaltyMemberTierList ){
        this.loyaltyMemberTierList = loyaltyMemberTierList;
    }

    public Boolean getIsLoyaltyMember(){
        return isLoyaltyMember;
    }

    public void setIsLoyaltyMember( Boolean isLoyaltyMember ){
        this.isLoyaltyMember = isLoyaltyMember;
    }

}