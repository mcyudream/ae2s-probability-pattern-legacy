package com.tz.statpatterns.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StatPatternsExtractorTest {
    public static class Getter { public int getChance() { return 80; } }
    public static class Field { private final double probability = .25; }
    public static class Wrapper { private final Field recipe = new Field(); }
    public static class Cycle { public Cycle getRecipe() { return this; } }
    @Test void getsValuesAndUnwrapsRecipeViewers() {
        assertEquals(.8, StatPatternsExtractor.extract(new Getter()).orElseThrow());
        assertEquals(.25, StatPatternsExtractor.extract(new Field()).orElseThrow());
        assertEquals(.25, StatPatternsExtractor.extract(new Wrapper()).orElseThrow());
    }
    @Test void ignoresUnsupportedValuesAndCycles() {
        for (var value : new Object[] { new Object(), Double.NaN, Double.POSITIVE_INFINITY, 0, -1, 101, new Cycle() })
            assertTrue(StatPatternsExtractor.extract(value).isEmpty());
    }
}
