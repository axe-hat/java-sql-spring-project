package com.hiring.sqlchallenge.service;

import com.hiring.sqlchallenge.client.HiringApiClient;
import com.hiring.sqlchallenge.config.ChallengeProperties;
import com.hiring.sqlchallenge.model.SolutionRequest;
import com.hiring.sqlchallenge.model.WebhookRequest;
import com.hiring.sqlchallenge.model.WebhookResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the three steps of the challenge: register, choose the query for
 * this registration number, and submit it to the returned webhook.
 */
@Service
public class ChallengeService {

    private static final Logger log = LoggerFactory.getLogger(ChallengeService.class);

    private final HiringApiClient client;
    private final SqlSolutionProvider solutionProvider;
    private final ChallengeProperties props;

    public ChallengeService(HiringApiClient client, SqlSolutionProvider solutionProvider,
                            ChallengeProperties props) {
        this.client = client;
        this.solutionProvider = solutionProvider;
        this.props = props;
    }

    public String execute() {
        ChallengeProperties.Candidate candidate = props.getCandidate();

        WebhookResponse registration = client.generateWebhook(
                new WebhookRequest(candidate.getName(), candidate.getRegNo(), candidate.getEmail()));

        String query = solutionProvider.forRegNo(candidate.getRegNo());
        log.info("Selected question {} from the registration number",
                solutionProvider.questionFor(candidate.getRegNo()));

        String result = client.submitSolution(
                registration.webhook(), registration.accessToken(), new SolutionRequest(query));

        log.info("Submission accepted");
        return result;
    }
}
