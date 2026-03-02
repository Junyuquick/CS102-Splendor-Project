package model;

public enum GemColor {
    WHITE, 
    BLUE, 
    GREEN, 
    RED, 
    BLACK, 
    GOLD;   // wildcard

public boolean isWildCard() {
    return this == GOLD;
}

}
