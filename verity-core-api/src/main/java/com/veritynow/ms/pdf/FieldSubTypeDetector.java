package com.veritynow.ms.pdf;
public final class FieldSubTypeDetector {

    private FieldSubTypeDetector() {}

    public static String detectSubType(String rawName, String fieldType) {
        if (rawName == null) return "text";

        // 1) Remove radio/checkbox value part: "A1 Existing Customer::Yes" -> "A1 Existing Customer"
        String name = rawName.split("::", 2)[0].trim();

        // 2) Split into tokens, keep letters/digits only
        String[] tokens = name.toLowerCase()
                .replaceAll("[^\\p{L}\\p{Nd}]+", " ")
                .trim()
                .split("\\s+");
        if (tokens.length == 0) return "text";

        // 3) Drop the A1 / A2 / A3 prefixes if present
        int start = 0;
        if (tokens[0].matches("a\\d+")) {
            start = 1;
        }
        if (start >= tokens.length) return "text";

        String last = tokens[tokens.length - 1];
        String prev  = tokens.length - 1 > start ? tokens[tokens.length - 2] : "";
        String prev2 = tokens.length - 2 > start ? tokens[tokens.length - 3] : "";

        // ---- Direct rules on the last word ---------------------------------

        switch (last) {
            case "month":   return "date_month";
            case "day":     return "date_day";
            case "year":    return "date_year";

            case "city":        return "city";
            case "country":     return "country";
            case "barangay":    return "barangay";
            case "street":      return "street";
            case "province":    return "province"; // or your own subtype if you want

            case "number":
                // Could be phone, ID, account – refine by previous words:
                if ("mobile".equals(prev) || "cellphone".equals(prev) || "personal".equals(prev)) {
                    return "mobilenumber";
                }
                if ("landline".equals(prev) || "home".equals(prev) || "business".equals(prev)) {
                    return "landlinenumber";
                }
                if ("account".equals(prev) || "cif".equals(prev) || "id1".equals(prev) || "id2".equals(prev)) {
                    return "number";
                }
                return "number";

            case "income":
            case "amount":
            case "tin":
            case "cif":
                return "number";

            case "email":
            case "emailaddress":
                return "email";

            case "zipcode":
            case "zip":
                return "zipcode";

            case "code":
                if ("zip".equals(prev)) return "zipcode";
                if ("area".equals(prev)) return "areacode";
                if ("country".equals(prev)) return "countrycode";
                return "text";

            case "mobile":
                return "mobilenumber";

            case "landline":
                return "landlinenumber";
        }

        // ---- Multi-word patterns (previous + last) -------------------------

        if ("mobile".equals(prev) && "number".equals(last)) {
            return "mobilenumber";
        }
        if ("landline".equals(prev) && "number".equals(last)) {
            return "landlinenumber";
        }
        if ("email".equals(prev) && "address".equals(last)) {
            return "email";
        }
        if ("zip".equals(prev) && "code".equals(last)) {
            return "zipcode";
        }
        if ("country".equals(prev) && "code".equals(last)) {
            return "countrycode";
        }
        if ("area".equals(prev) && "code".equals(last)) {
            return "areacode";
        }

        // Example: "Gross Monthly Income" -> last="income"
        if ("gross".equals(prev2) && "income".equals(last)) {
            return "number"; // or "currency" if you want a special subtype
        }

        // ---- Fall back based on fieldType (checkbox/radio vs text) ---------

        if ("checkbox".equalsIgnoreCase(fieldType) || "radio".equalsIgnoreCase(fieldType)) {
            return "choice";
        }

        // Default
        return "text";
    }
}
