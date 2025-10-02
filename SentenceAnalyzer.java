// pww-core/src/main/java/com/pww/core/SentenceAnalyzer.java
package com.pww.core;

import com.pww.core.model.*;
import com.pww.core.io.CorpusSource;
import com.pww.core.phrase.PhraseMatcher;

import java.util.*;

public class SentenceAnalyzer {
    private final CorpusSource corpus;
    private final PhraseMatcher matcher;

    // Working state (replaces global arrays in spec)
    private List<String> completeSentence;   // Complete_Text_Sentence
    private List<String> estimatedSentence;  // Estimated_Text_Sentence
    private int npaw;                        // number present analyzed word (1-based in spec)
    private final AnalysisStats stats = new AnalysisStats();

    public SentenceAnalyzer(CorpusSource corpus, PhraseMatcher matcher) {
        this.corpus = corpus;
        this.matcher = matcher;
    }

    public AnalysisResult analyzeNSentences(int totalNumSentences) {
        int sentencesAnalyzed = 0;
        while (sentencesAnalyzed < totalNumSentences) {
            // Sentence Analysis SUBROUTINE
            List<String> s = corpus.pickRandomSentence();
            if (s == null || s.size() < 5) continue; // spec: skip sentences with <5 words

            completeSentence = s;
            estimatedSentence = new ArrayList<>(Collections.nCopies(s.size(), ""));
            npaw = 0;

            // initialize totals
            for (String w : s) stats.totalNumCTSChars += w.length(); // 

            // Step 2 loop: first four words handling
            for (; npaw < Math.min(4, s.size()); ) {
                step2FirstFourWords();
            }

            // Matching Phrase Analysis + datasets and “Estimated Text Sentence SUBROUTINE”
            if (s.size() == 4) {
                completeEstimatedTextSentence(); // 
            } else {
                estimatedTextSentenceLoop(); 
                phraseAnalysisLoop();            
            }

            // Final Phrase Analysis when needed (3-char PW4) 

            completeEstimatedTextSentence();     // tally CTS/ETS, correct/wrong, etc. 

            // update sentence totals from spec
            stats.totalNumWordsAnalyzed += (s.size() - 4); // 
            sentencesAnalyzed++;
        }

        return AnalysisResult.from(stats);
    }

    private void step2FirstFourWords() {
        npaw++; // “Add one to NPAW” 
        String word = completeSentence.get(npaw - 1);
        int len = word.length();

        if (len == 1) { stats.totalNumWrittenChars += 1; estimatedSentence.set(npaw - 1, word); /* Whole=True */ }  
        else if (len == 2) { stats.totalNumWrittenChars += 2.5; estimatedSentence.set(npaw - 1, word); }                 
        else if (len == 3) { stats.totalNumWrittenChars += 3.5; estimatedSentence.set(npaw - 1, word); }
        else { stats.totalNumWrittenChars += 4; estimatedSentence.set(npaw - 1, word.substring(0, 4)); }
    }

    private void estimatedTextSentenceLoop() {
        while (true) {
            npaw++;
            if (npaw == completeSentence.size()) { npaw = 3; break; } // goto Phrase Analysis 
            String w = completeSentence.get(npaw - 1);
            int len = w.length();
            stats.totalNumCTSChars += len;
            if (len > 1) { stats.totalNumInputtedChars += 2; estimatedSentence.set(npaw - 1, w.substring(0, 2)); }
            else { stats.totalNumInputtedChars += 1; estimatedSentence.set(npaw - 1, w); } 
        }
    }

    private void phraseAnalysisLoop() {
        while (true) {
            npaw++;
            if (npaw == completeSentence.size()) { finalPhraseAnalysis(); return; }
            String w = completeSentence.get(npaw - 1);
            if (w.length() == 1) continue; 

            // Build PartialWordListEntry with PW4=2 chars here 
            PartialPhrase pp = new PartialPhrase(
                    estimatedSentence.get(npaw - 4),
                    estimatedSentence.get(npaw - 3),
                    estimatedSentence.get(npaw - 2),
                    estimatedSentence.get(npaw - 1), // PW4
                    2
            );
            var hits = matcher.match(pp); // Matching Phrase Analysis 
            // choose highest repeats ⇒ Third_Word
            var best = hits.stream().max(Comparator.comparingInt(WholePhraseHit::repeats)).orElse(null);
            if (best != null) {
                estimatedSentence.set(npaw - 1, best.w3()); // “Estimated_Text_Sentence(NPAW) = Third_Word”
            }
        }
    }

    private void finalPhraseAnalysis() {
        String w = completeSentence.get(npaw - 1);
        int len = w.length();
        if (len == 1) { stats.totalNumWrittenChars += 1; completeEstimatedTextSentence(); return; }
        if (len == 2) { stats.totalNumWrittenChars += 2.5; completeEstimatedTextSentence(); return;}

        // Else, PW4 = first 3 chars; Total_Num_Partial_Characters=3 
        var pp = new PartialPhrase(
                estimatedSentence.get(npaw - 4),
                estimatedSentence.get(npaw - 3),
                estimatedSentence.get(npaw - 2),
                w.substring(0, Math.min(3, w.length())),
                3
        );
        stats.totalNumWrittenChars += 3;
        var hits = matcher.match(pp);
        var first = hits.stream().findFirst().orElse(null); // “Determine the fourth word of first sublist entry.”
        if (first != null) estimatedSentence.set(npaw - 1, first.w4());
        completeEstimatedTextSentence();
    }

    private void completeEstimatedTextSentence() {
        // Build CTS/ETS and compute correct/wrong
        for (int i = 0; i < completeSentence.size(); i++) {
            if ((i > 3) && (i < completeSentence.size())) {
                if (Objects.equals(estimatedSentence.get(i), completeSentence.get(i))) stats.totalNumCorrectWords++;
                else stats.totalNumWrongWords++;
            }
        }
    }
}
