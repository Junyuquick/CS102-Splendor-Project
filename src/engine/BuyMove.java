package engine;

import model.DevelopmentCard;
import model.GemColor;

import java.util.HashMap;
import java.util.Map;

final class BuyMove extends Move {
    private final DevelopmentCard card;
    private final Map<GemColor, Integer> paymentTokens;
    private final boolean fromReserved;

    BuyMove(DevelopmentCard card, Map<GemColor, Integer> paymentTokens, boolean fromReserved) {
        this.card = card;
        this.paymentTokens = new HashMap<>(paymentTokens);
        this.fromReserved = fromReserved;
    }

    @Override
    public String getTypeName() {
        return "BUY";
    }

    @Override
    public DevelopmentCard getCard() {
        return card;
    }
    
    @Override
    public Map<GemColor, Integer> getPaymentTokens() {
        return new HashMap<>(paymentTokens);
    }

    @Override 
    // this method is used to determine if the card being bought is from the player's reserved cards or from the market
    // if true then the card is from the player's reserved cards, if false then the card is from the market
    public boolean isFromReserved() {
        return fromReserved;
    }
}
