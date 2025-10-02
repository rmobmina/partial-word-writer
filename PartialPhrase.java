// pww-core/src/main/java/com/pww/core/model/PartialPhrase.java
package com.pww.core.model;

import java.util.Objects;

public record PartialPhrase(String w1, String w2, String w3, String partial4, int partialChars) {
    public String key() { // canonical key for caching / lookup
        return (w1 + " " + w2 + " " + w3 + " " + partial4).toLowerCase();
    }
}
