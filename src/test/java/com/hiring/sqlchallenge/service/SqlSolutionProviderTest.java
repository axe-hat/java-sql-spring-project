package com.hiring.sqlchallenge.service;

import com.hiring.sqlchallenge.exception.ChallengeException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SqlSolutionProviderTest {

    private final SqlSolutionProvider provider = new SqlSolutionProvider();

    @Test
    void extractsTrailingTwoDigits() {
        assertThat(provider.lastTwoDigits("TST0042")).isEqualTo(42);
        assertThat(provider.lastTwoDigits("TST0043")).isEqualTo(43);
        assertThat(provider.lastTwoDigits("REG7")).isEqualTo(7);
    }

    @Test
    void rejectsRegNoWithoutDigits() {
        assertThatThrownBy(() -> provider.lastTwoDigits("ABC"))
                .isInstanceOf(ChallengeException.class);
        assertThatThrownBy(() -> provider.lastTwoDigits(null))
                .isInstanceOf(ChallengeException.class);
    }

    @Test
    void oddRegNoGetsQuestionOne() {
        String query = provider.forRegNo("TST0043");
        assertThat(query).contains("SALARY").contains("ORDER BY p.AMOUNT DESC");
    }

    @Test
    void evenRegNoGetsQuestionTwo() {
        String query = provider.forRegNo("TST0042");
        assertThat(query).contains("YOUNGER_EMPLOYEES_COUNT");
    }

    @Test
    void loadedQueryIsTrimmedNotEmpty() {
        String query = provider.forRegNo("TST0042");
        assertThat(query).isNotBlank();
        assertThat(query).doesNotEndWith("\n");
    }
}
