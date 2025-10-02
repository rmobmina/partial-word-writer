// pww-core/src/main/java/com/pww/core/phrase/PhraseMatcher.java
package com.pww.core.phrase;

import com.pww.core.model.PartialPhrase;
import com.pww.core.model.WholePhraseHit;
import java.util.List;

public interface PhraseMatcher {
    /**
     * Spec: “The first 3 whole words must match and the 4th partial must match the first N characters of the 4th word.”
     * Returns sublist entries with current repeats, sorted later.
     */
    List<WholePhraseHit> match(PartialPhrase pp);
}
