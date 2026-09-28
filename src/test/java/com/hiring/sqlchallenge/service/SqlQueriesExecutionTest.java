package com.hiring.sqlchallenge.service;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the two SQL answers exactly as they are submitted, against the challenge
 * schema in an in-memory H2 database (MySQL compatibility mode), and checks the
 * results on a fixture built around each question's edge cases.
 */
class SqlQueriesExecutionTest {

    private static DriverManagerDataSource dataSource;
    private static JdbcTemplate jdbc;
    private final SqlSolutionProvider provider = new SqlSolutionProvider();

    @BeforeAll
    static void createDatabase() {
        dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:challenge;MODE=MySQL;DATABASE_TO_UPPER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(
                new ClassPathResource("sql/schema.sql"), new ClassPathResource("sql/data.sql"))
                .execute(dataSource);
        jdbc = new JdbcTemplate(dataSource);
    }

    @AfterAll
    static void dropDatabase() {
        jdbc.execute("DROP ALL OBJECTS");
    }

    @Test
    void questionOneReturnsHighestPaymentNotMadeOnTheFirst() {
        List<Map<String, Object>> rows = jdbc.queryForList(provider.forRegNo("TST0043"));

        assertThat(rows).hasSize(1);
        Map<String, Object> row = rows.get(0);
        assertThat(row.keySet()).containsExactly("SALARY", "NAME", "AGE", "DEPARTMENT_NAME");
        // 99999.00 (paid on 3/1) and 91000.00 (paid on 4/1, just after midnight) are excluded
        assertThat((BigDecimal) row.get("SALARY")).isEqualByComparingTo("88000.00");
        assertThat(row.get("NAME")).isEqualTo("Sarah Johnson");
        assertThat(((Number) row.get("AGE")).intValue()).isEqualTo(LocalDate.now().getYear() - 1990);
        assertThat(row.get("DEPARTMENT_NAME")).isEqualTo("Finance");
    }

    @Test
    void questionTwoCountsYoungerColleaguesPerDepartment() {
        List<Map<String, Object>> rows = jdbc.queryForList(provider.forRegNo("TST0042"));

        assertThat(rows).extracting(r -> ((Number) r.get("EMP_ID")).intValue())
                .containsExactly(6, 5, 4, 3, 2, 1);              // ORDER BY EMP_ID DESC
        assertThat(rows.get(0).keySet()).containsExactly(
                "EMP_ID", "FIRST_NAME", "LAST_NAME", "DEPARTMENT_NAME", "YOUNGER_EMPLOYEES_COUNT");

        Map<Integer, Integer> younger = rows.stream().collect(java.util.stream.Collectors.toMap(
                r -> ((Number) r.get("EMP_ID")).intValue(),
                r -> ((Number) r.get("YOUNGER_EMPLOYEES_COUNT")).intValue()));
        // Engineering: John 1980 < Michael = Olivia 1985-08-10 < David 1988
        assertThat(younger).containsEntry(1, 3)   // Michael, Olivia, David are younger
                .containsEntry(3, 1)              // only David; Olivia shares the birthday
                .containsEntry(6, 1)              // only David; Michael shares the birthday
                .containsEntry(5, 0)              // youngest in Engineering
                .containsEntry(2, 0)              // alone in Finance
                .containsEntry(4, 0);             // alone in HR
        assertThat(rows).filteredOn(r -> ((Number) r.get("EMP_ID")).intValue() == 1)
                .singleElement()
                .satisfies(r -> assertThat(r.get("DEPARTMENT_NAME")).isEqualTo("Engineering"));
    }
}
