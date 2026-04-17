package io.github.yamatrireddy;

import io.github.yamatrireddy.api.ParserConfig;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class ParserConfigTest {

    @Test
    void defaultsHaveExpectedValues() {
        ParserConfig cfg = ParserConfig.defaults();
        assertThat(cfg.getMaxDepth()).isEqualTo(10);
        assertThat(cfg.getMaxValueLength()).isEqualTo(128);
        assertThat(cfg.getMaxWildcardsPerValue()).isEqualTo(2);
        assertThat(cfg.getMaxInputLength()).isEqualTo(4096);
        assertThat(cfg.getAllowedOperators()).containsExactlyInAnyOrder(
                "==", "!=", ">", ">=", "<", "<=", "=in=", "=out=");
    }

    @Test
    void builderOverridesAllValues() {
        ParserConfig cfg = ParserConfig.builder()
                .maxDepth(3)
                .maxValueLength(32)
                .maxWildcardsPerValue(0)
                .maxInputLength(256)
                .allowedOperators(Set.of("==", "!="))
                .build();

        assertThat(cfg.getMaxDepth()).isEqualTo(3);
        assertThat(cfg.getMaxValueLength()).isEqualTo(32);
        assertThat(cfg.getMaxWildcardsPerValue()).isEqualTo(0);
        assertThat(cfg.getMaxInputLength()).isEqualTo(256);
        assertThat(cfg.getAllowedOperators()).containsExactlyInAnyOrder("==", "!=");
    }

    @Test
    void builderRejectsZeroMaxDepth() {
        assertThatThrownBy(() -> ParserConfig.builder().maxDepth(0).build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void builderRejectsEmptyOperatorSet() {
        assertThatThrownBy(() -> ParserConfig.builder().allowedOperators(Set.of()).build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void allowedOperatorsIsImmutable() {
        ParserConfig cfg = ParserConfig.defaults();
        assertThatThrownBy(() -> cfg.getAllowedOperators().add("=custom="))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
