package com.playtheatria.joinvelocity.enums;

import net.kyori.adventure.text.format.NamedTextColor;

public enum Color {
    DARK_AQUA("DARK_AQUA", NamedTextColor.DARK_AQUA),
    DARK_GRAY("DARK_GRAY", NamedTextColor.DARK_GRAY),
    GRAY("GRAY", NamedTextColor.GRAY),
    GREEN("GREEN", NamedTextColor.GREEN);

    private final String value;

    private final NamedTextColor namedTextColor;

    Color(String value, NamedTextColor namedTextColor) {
        this.value = value;
        this.namedTextColor = namedTextColor;
    }

    private String getValue() {
        return value;
    }

    private NamedTextColor getNamedTextColor() {
        return namedTextColor;
    }

    public static NamedTextColor fromValue(String value) throws IllegalArgumentException {
        for (Color color : Color.values()) {
            if (color.getValue().equals(value)) {
                return color.getNamedTextColor();
            }
        }
        throw new IllegalArgumentException("Invalid value: " + value);
    };
}
