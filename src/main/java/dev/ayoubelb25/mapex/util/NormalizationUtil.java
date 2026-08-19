package dev.ayoubelb25.mapex.util;

public class NormalizationUtil {

    private NormalizationUtil() {
    }

    public static String normalizeSide(String raw) {
        return switch (raw.toUpperCase()) {
            case "1", "BUY", "B", "A", "ACHAT" ->
                "1";
            case "-1", "SELL", "S", "V", "VENTE" ->
                "-1";
            default ->
                raw;
        };
    }

    public static String normalizeOrderType(String raw) {
        return switch (raw.trim().toUpperCase()) {
            case "M", "MKT", "MP", "MARKET", "AU-MARCHE", "AU MARCHÉ", "AU MARCHE", "AU-MARCHÉ" ->
                "M";
            case "L", "LMT", "LM", "LIMIT", "LIMITE", "LIMITÉ" ->
                "L";
            default ->
                raw;
        };
    }

}
