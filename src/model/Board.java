package model;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Board implements Serializable {

    private final Map<Integer, List<DevelopmentCard>> faceUpCards;
    private final List<NobleTile> nobles;
    private final Map<GemColor, Integer> gems;

    // Internal decks used to refill face-up slots and support blind draws.
    private final Map<Integer, Deque<DevelopmentCard>> decks;
    // Stores last removed face-up slot index per tier so refills preserve board order.
    private final Map<Integer, Integer> lastRemovedFaceUpIndexByTier;
    private final GemBank bank;
    private final int openCardsPerTier;

    public Board() {
        this(new HashMap<>(), new ArrayList<>(), new EnumMap<>(GemColor.class), new GemBank(), 4);
    }

    public Board(Map<Integer, List<DevelopmentCard>> deckCards,
                 List<NobleTile> nobles,
                 Map<GemColor, Integer> initialGems,
                 GemBank bank,
                 int openCardsPerTier) {
        if (openCardsPerTier <= 0) {
            throw new IllegalArgumentException("openCardsPerTier must be > 0");
        }

        this.faceUpCards = new HashMap<>();
        this.nobles = new ArrayList<>();
        this.gems = new EnumMap<>(GemColor.class);
        this.decks = new HashMap<>();
        this.lastRemovedFaceUpIndexByTier = new HashMap<>();
        this.bank = Objects.requireNonNull(bank, "bank cannot be null");
        this.openCardsPerTier = openCardsPerTier;

        for (int tier = 1; tier <= 3; tier++) {
            this.faceUpCards.put(tier, new ArrayList<>());
            List<DevelopmentCard> tierDeck = deckCards == null
                    ? Collections.emptyList()
                    : deckCards.getOrDefault(tier, Collections.emptyList());
            this.decks.put(tier, new ArrayDeque<>(tierDeck));
        }

        initializeGemSupply(initialGems);
        initializeNobles(nobles);
        initializeCards();
    }

    private void initializeCards() {
        for (int tier = 1; tier <= 3; tier++) {
            List<DevelopmentCard> visible = faceUpCards.get(tier);
            while (visible.size() < openCardsPerTier) {
                DevelopmentCard next = drawFromDeck(tier);
                if (next == null) {
                    break;
                }
                visible.add(next);
            }
        }
    }

    private void initializeGemSupply() {
        initializeGemSupply(null);
    }

    private void initializeGemSupply(Map<GemColor, Integer> initialGems) {
        gems.clear();
        for (GemColor color : GemColor.values()) {
            int count = initialGems == null ? 0 : Math.max(0, initialGems.getOrDefault(color, 0));
            gems.put(color, count);
            int bankCount = bank.getTokenCount(color);
            if (bankCount < count) {
                bank.addGems(color, count - bankCount);
            } else if (bankCount > count) {
                bank.removeGems(color, bankCount - count);
            }
        }
    }

    private void initializeNobles() {
        initializeNobles(Collections.emptyList());
    }

    private void initializeNobles(List<NobleTile> initialNobles) {
        nobles.clear();
        if (initialNobles != null) {
            nobles.addAll(initialNobles);
        }
    }

    public void takeGems(Map<GemColor, Integer> requested) {
        if (requested == null || requested.isEmpty()) {
            return;
        }

        for (Map.Entry<GemColor, Integer> entry : requested.entrySet()) {
            GemColor color = entry.getKey();
            int amount = entry.getValue() == null ? 0 : entry.getValue();
            if (amount < 0) {
                throw new IllegalArgumentException("Requested amount cannot be negative");
            }
            if (getGemCount(color) < amount) {
                throw new IllegalArgumentException("Not enough gems available for " + color);
            }
        }

        for (Map.Entry<GemColor, Integer> entry : requested.entrySet()) {
            GemColor color = entry.getKey();
            int amount = entry.getValue();
            gems.put(color, getGemCount(color) - amount);
            bank.removeGems(color, amount);
        }
    }

    public void returnGems(Map<GemColor, Integer> returned) {
        if (returned == null || returned.isEmpty()) {
            return;
        }

        for (Map.Entry<GemColor, Integer> entry : returned.entrySet()) {
            GemColor color = entry.getKey();
            int amount = entry.getValue() == null ? 0 : entry.getValue();
            if (amount < 0) {
                throw new IllegalArgumentException("Returned amount cannot be negative");
            }
            gems.put(color, getGemCount(color) + amount);
            bank.addGems(color, amount);
        }
    }

    public DevelopmentCard purchaseCard(int tier, int index) {
        validateTier(tier);
        List<DevelopmentCard> tierCards = faceUpCards.get(tier);
        if (index < 0 || index >= tierCards.size()) {
            throw new IndexOutOfBoundsException("Invalid card index for tier " + tier + ": " + index);
        }

        DevelopmentCard purchased = tierCards.remove(index);
        DevelopmentCard refill = drawFromDeck(tier);
        if (refill != null) {
            tierCards.add(index, refill);
        }
        return purchased;
    }

    public DevelopmentCard drawFromDeck(int tier) {
        validateTier(tier);
        Deque<DevelopmentCard> deck = decks.get(tier);
        return deck == null ? null : deck.pollFirst();
    }

    public void removeNoble(NobleTile noble) {
        nobles.remove(noble);
    }

    public int getGemCount(GemColor color) {
        return gems.getOrDefault(color, 0);
    }

    public List<DevelopmentCard> getFaceUpCards(int tier) {
        validateTier(tier);
        return Collections.unmodifiableList(faceUpCards.get(tier));
    }

    public List<NobleTile> getAvailableNobles() {
        return Collections.unmodifiableList(nobles);
    }

    // Compatibility overload used in engine classes.
    public List<DevelopmentCard> getFaceUpCards() {
        List<DevelopmentCard> all = new ArrayList<>();
        for (int tier = 1; tier <= 3; tier++) {
            all.addAll(faceUpCards.getOrDefault(tier, Collections.emptyList()));
        }
        return Collections.unmodifiableList(all);
    }

    // Compatibility method used in engine classes.
    public void removeCard(DevelopmentCard card) {
        if (card == null) {
            return;
        }
        for (int tier = 1; tier <= 3; tier++) {
            List<DevelopmentCard> visible = faceUpCards.get(tier);
            int index = visible.indexOf(card);
            if (index >= 0) {
                visible.remove(index);
                lastRemovedFaceUpIndexByTier.put(tier, index);
                return;
            }
        }
        for (int tier = 1; tier <= 3; tier++) {
            Deque<DevelopmentCard> deck = decks.get(tier);
            if (deck != null && deck.remove(card)) {
                return;
            }
        }
    }

    // Compatibility method used in engine classes.
    public void refillSlot(DevelopmentCard removedCard) {
        if (removedCard == null) {
            return;
        }
        int tier = inferTier(removedCard);
        if (tier < 1) {
            return;
        }
        List<DevelopmentCard> tierCards = faceUpCards.get(tier);
        if (tierCards.size() >= openCardsPerTier) {
            return;
        }
        DevelopmentCard refill = drawFromDeck(tier);
        if (refill != null) {
            Integer index = lastRemovedFaceUpIndexByTier.remove(tier);
            if (index != null && index >= 0 && index <= tierCards.size()) {
                tierCards.add(index, refill);
            } else {
                tierCards.add(refill);
            }
        }
    }

    private int inferTier(DevelopmentCard card) {
        String level = String.valueOf(card.getLevel()).toUpperCase();
        if (level.contains("1")) return 1;
        if (level.contains("2")) return 2;
        if (level.contains("3")) return 3;
        return -1;
    }

    private void validateTier(int tier) {
        if (tier < 1 || tier > 3) {
            throw new IllegalArgumentException("Tier must be 1, 2, or 3");
        }
    }
}
