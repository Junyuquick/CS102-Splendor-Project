package model;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the shared board state for a Splendor game.
 *
 * The board tracks the visible market, the remaining decks for each
 * tier, available nobles, and the token supply mirrored in GemBank.
 */
public class Board implements Serializable {

    private final Map<Integer, List<DevelopmentCard>> faceUpCards;
    private final List<NobleTile> nobles;
    private final Map<GemColor, Integer> gems;
    private final Map<Integer, Deque<DevelopmentCard>> decks;
    private final Map<Integer, Integer> lastRemovedFaceUpIndexByTier;
    private final GemBank bank;
    private final int openCardsPerTier;

    /**
     * Creates an empty board with the default four visible cards per tier.
     */
    public Board() {
        this(
                new HashMap<>(),
                new ArrayList<>(),
                new LinkedHashMap<>(),
                new GemBank(),
                4
        );
    }

    /**
     * Creates a board from prepared decks, nobles, and token counts.
     *
     * @param deckCards cards remaining in each tier deck, ordered from
     *     next draw onward
     * @param nobles nobles currently available on the board
     * @param initialGems initial token counts shown on the board
     * @param bank shared bank used to mirror token inventory
     * @param openCardsPerTier number of face-up cards to maintain per tier
     */
    public Board(
            Map<Integer, List<DevelopmentCard>> deckCards,
            List<NobleTile> nobles,
            Map<GemColor, Integer> initialGems,
            GemBank bank,
            int openCardsPerTier
    ) {
        if (openCardsPerTier <= 0) {
            throw new IllegalArgumentException("openCardsPerTier must be > 0");
        }

        this.faceUpCards = new HashMap<>();
        this.nobles = new ArrayList<>();
        this.gems = new LinkedHashMap<>();
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

    private void initializeGemSupply(Map<GemColor, Integer> initialGems) {
        gems.clear();
        for (GemColor color : GemColor.values()) {
            int count = initialGems == null
                    ? 0
                    : Math.max(0, initialGems.getOrDefault(color, 0));
            gems.put(color, count);

            int bankCount = bank.getTokenCount(color);
            if (bankCount < count) {
                bank.addGems(color, count - bankCount);
            } else if (bankCount > count) {
                bank.removeGems(color, bankCount - count);
            }
        }
    }

    private void initializeNobles(List<NobleTile> initialNobles) {
        nobles.clear();
        if (initialNobles != null) {
            nobles.addAll(initialNobles);
        }
    }

    /**
     * Removes tokens from the board supply.
     *
     * @param requested number of tokens requested for each color
     * @throws IllegalArgumentException if a request is negative or
     *     exceeds the available supply
     */
    public void takeGems(Map<GemColor, Integer> requested) {
        if (requested == null || requested.isEmpty()) {
            return;
        }

        for (Map.Entry<GemColor, Integer> entry : requested.entrySet()) {
            GemColor color = entry.getKey();
            int amount = entry.getValue() == null ? 0 : entry.getValue();
            if (amount < 0) {
                throw new IllegalArgumentException(
                        "Requested amount cannot be negative"
                );
            }
            if (getGemCount(color) < amount) {
                throw new IllegalArgumentException(
                        "Not enough gems available for " + color
                );
            }
        }

        for (Map.Entry<GemColor, Integer> entry : requested.entrySet()) {
            GemColor color = entry.getKey();
            int amount = entry.getValue();
            gems.put(color, getGemCount(color) - amount);
            bank.removeGems(color, amount);
        }
    }

    /**
     * Returns tokens to the board supply.
     *
     * @param returned number of tokens returned for each color
     * @throws IllegalArgumentException if a returned amount is negative
     */
    public void returnGems(Map<GemColor, Integer> returned) {
        if (returned == null || returned.isEmpty()) {
            return;
        }

        for (Map.Entry<GemColor, Integer> entry : returned.entrySet()) {
            GemColor color = entry.getKey();
            int amount = entry.getValue() == null ? 0 : entry.getValue();
            if (amount < 0) {
                throw new IllegalArgumentException(
                        "Returned amount cannot be negative"
                );
            }
            gems.put(color, getGemCount(color) + amount);
            bank.addGems(color, amount);
        }
    }

    /**
     * Removes a face-up card and refills its slot when possible.
     *
     * @param tier development-card tier
     * @param index zero-based index within that tier's visible cards
     * @return the purchased card
     * @throws IllegalArgumentException if the tier is unsupported
     * @throws IndexOutOfBoundsException if the index is outside the
     *     visible-card range
     */
    public DevelopmentCard purchaseCard(int tier, int index) {
        validateTier(tier);
        List<DevelopmentCard> tierCards = faceUpCards.get(tier);
        if (index < 0 || index >= tierCards.size()) {
            throw new IndexOutOfBoundsException(
                    "Invalid card index for tier " + tier + ": " + index
            );
        }

        DevelopmentCard purchased = tierCards.remove(index);
        DevelopmentCard refill = drawFromDeck(tier);
        if (refill != null) {
            tierCards.add(index, refill);
        }
        return purchased;
    }

    /**
     * Draws the next hidden card from the requested tier.
     *
     * @param tier development-card tier
     * @return the next card, or null if the deck is empty
     * @throws IllegalArgumentException if the tier is unsupported
     */
    public DevelopmentCard drawFromDeck(int tier) {
        validateTier(tier);
        Deque<DevelopmentCard> deck = decks.get(tier);
        return deck == null ? null : deck.pollFirst();
    }

    /**
     * Removes a noble from the list of available nobles.
     *
     * @param noble noble to remove
     */
    public void removeNoble(NobleTile noble) {
        nobles.remove(noble);
    }

    /**
     * Returns the number of available tokens of a given color.
     *
     * @param color token color to query
     * @return available token count for that color
     */
    public int getGemCount(GemColor color) {
        return gems.getOrDefault(color, 0);
    }

    /**
     * Returns the visible cards for one tier.
     *
     * @param tier development-card tier
     * @return an unmodifiable view of the visible cards in that tier
     * @throws IllegalArgumentException if the tier is unsupported
     */
    public List<DevelopmentCard> getFaceUpCards(int tier) {
        validateTier(tier);
        return Collections.unmodifiableList(faceUpCards.get(tier));
    }

    /**
     * Returns the nobles still available on the board.
     *
     * @return an unmodifiable view of the available nobles
     */
    public List<NobleTile> getAvailableNobles() {
        return Collections.unmodifiableList(nobles);
    }

    /**
     * Returns all visible cards across every tier.
     *
     * @return an unmodifiable flattened view of all visible cards
     */
    public List<DevelopmentCard> getFaceUpCards() {
        List<DevelopmentCard> all = new ArrayList<>();
        for (int tier = 1; tier <= 3; tier++) {
            all.addAll(faceUpCards.getOrDefault(tier, Collections.emptyList()));
        }
        return Collections.unmodifiableList(all);
    }

    /**
     * Removes a card from the visible market or from a hidden deck.
     *
     * If the card is removed from the visible market, the removed slot
     * index is remembered so that a later refill can preserve the
     * original ordering of the visible cards.
     *
     * @param card card to remove
     */
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

    /**
     * Refills the face-up slot for a removed card.
     *
     * @param removedCard card whose tier determines the slot to refill
     */
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

    /**
     * Infers a card's tier from its recorded level value.
     *
     * @param card card whose tier should be determined
     * @return inferred tier, or -1 if no supported tier can be found
     */
    private int inferTier(DevelopmentCard card) {
        String level = String.valueOf(card.getLevel()).toUpperCase();
        if (level.contains("1")) {
            return 1;
        }
        if (level.contains("2")) {
            return 2;
        }
        if (level.contains("3")) {
            return 3;
        }
        return -1;
    }

    /**
     * Validates that a tier value is within the supported range.
     *
     * @param tier tier to validate
     * @throws IllegalArgumentException if the tier is unsupported
     */
    private void validateTier(int tier) {
        if (tier < 1 || tier > 3) {
            throw new IllegalArgumentException("Tier must be 1, 2, or 3");
        }
    }
}
