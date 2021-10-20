package io.quarkiverse.githubapp.runtime.health;

import io.quarkiverse.githubapp.runtime.github.GitHubService;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Liveness;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@Liveness
@ApplicationScoped
public class GitHubConnectionLivenessCheck implements HealthCheck {

    @Inject
    GitHubService gitHubService;

    @Override
    public HealthCheckResponse call() {
        gitHubService.getInstallationClient(TODO)
    }
}
