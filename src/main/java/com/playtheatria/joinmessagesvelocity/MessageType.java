package com.playtheatria.joinmessagesvelocity;

public enum MessageType {
    CONNECT("+"),
    DISCONNECT("-");

    private final String symbol;

    MessageType(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return this.symbol;
    }
}
