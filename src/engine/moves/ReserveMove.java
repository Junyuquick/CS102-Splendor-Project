package engine.moves;

import model.DevelopmentCard;

public final class ReserveMove extends Move {
    private final DevelopmentCard card;
    private final int cardLevel;

    ReserveMove(DevelopmentCard card, int cardLevel) {
        this.card = card;
        this.cardLevel = cardLevel;
    }

    @Override
    public String getTypeName() {
        return "RESERVE";
    }

    @Override
    public DevelopmentCard getCard() {
        return card;
    }

    @Override
    public int getCardLevel() {
        return cardLevel;
    }
}
