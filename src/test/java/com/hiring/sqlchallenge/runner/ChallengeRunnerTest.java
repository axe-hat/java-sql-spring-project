package com.hiring.sqlchallenge.runner;

import com.hiring.sqlchallenge.exception.ChallengeException;
import com.hiring.sqlchallenge.service.ChallengeService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChallengeRunnerTest {

    private final ChallengeService service = mock(ChallengeService.class);
    private final ChallengeRunner runner = new ChallengeRunner(service);

    @Test
    void runsTheChallengeOnce() {
        when(service.execute()).thenReturn("{\"success\":true}");

        assertThatCode(() -> runner.run(new DefaultApplicationArguments())).doesNotThrowAnyException();
        verify(service).execute();
    }

    @Test
    void failureIsRethrownSoTheProcessExitsNonZero() {
        when(service.execute()).thenThrow(new ChallengeException("boom"));

        assertThatThrownBy(() -> runner.run(new DefaultApplicationArguments()))
                .isInstanceOf(ChallengeException.class)
                .hasMessage("boom");
    }
}
