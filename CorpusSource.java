// pww-core/src/main/java/com/pww/core/io/CorpusSource.java
package com.pww.core.io;

import java.util.List;

public interface CorpusSource {
    /** Randomly returns a sentence (tokenized). Skip if words < 5. */
    List<String> pickRandomSentence();
    /** Find all 4-word phrases (w1 w2 w3 w4) in-sentence boundaries. */
    List<List<String>> findAllFourWordPhrases(); // optional if you pre-index
}
