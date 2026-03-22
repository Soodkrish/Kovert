package com.yourfamily.pdf.secure_pdf_converter.core.redaction.precision;

import java.util.*;

public final class RedactionHitMap {

    private final Map<Integer, Set<Integer>> pageToCharHits = new HashMap<>();

    public void add(RedactionHit hit) {
        pageToCharHits
                .computeIfAbsent(hit.pageIndex(), k -> new HashSet<>())
                .add(hit.charIndex());
    }

    public boolean isRedacted(int pageIndex, int charIndex) {
        return pageToCharHits
                .getOrDefault(pageIndex, Set.of())
                .contains(charIndex);
    }
    
    public boolean hasHitsOnPage(int pageIndex) {
        Set<Integer> hits = pageToCharHits.get(pageIndex);
        return hits != null && !hits.isEmpty();
    }
    
    public static RedactionHitMap fromHits(List<RedactionHit> hits) {
        RedactionHitMap map = new RedactionHitMap();
        for (RedactionHit hit : hits) {
            map.add(hit);
        }
        return map;
    }
}