package io.quarkiverse.githubapp.it.testingframework;

import static io.quarkiverse.githubapp.testing.GitHubAppTesting.given;
import static io.quarkiverse.githubapp.testing.GitHubAppTesting.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.ReactionContent;
import org.mockito.Mockito;

import io.quarkiverse.githubapp.testing.GitHubAppTestingResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(GitHubAppTestingResource.class)
public class TestingFrameworkTest {

    @Test
    void ghObjectMocking() {
        String[] capture = new String[1];
        IssueEventListener.behavior = (payload, configFile) -> {
            capture[0] = payload.getIssue().getBody();
        };
        assertThatCode(() -> given()
                .github(mocks -> {
                    Mockito.when(mocks.issue(750705278).getBody()).thenReturn("someValue");
                })
                .when().payloadFromClasspath("/issue-opened.json")
                .event(GHEvent.ISSUES)
                .then().github(mocks -> {
                }))
                        .doesNotThrowAnyException();
        assertThat(capture[0]).isEqualTo("someValue");
    }

    @Test
    void ghObjectVerify() {
        ThrowingCallable assertion = () -> given()
                .github(mocks -> {
                    Mockito.doNothing()
                            .when(mocks.issue(750705278)).addLabels(any(String.class));
                    Mockito.when(mocks.issue(750705278).getBody()).thenReturn("someValue");
                })
                .when().payloadFromClasspath("/issue-opened.json")
                .event(GHEvent.ISSUES)
                .then().github(mocks -> {
                    verify(mocks.issue(750705278))
                            .addLabels("someValue");
                    verifyNoMoreInteractions(mocks.ghObjects());
                });

        // Success
        IssueEventListener.behavior = (payload, configFile) -> {
            payload.getIssue().addLabels("someValue");
        };
        assertThatCode(assertion).doesNotThrowAnyException();

        // Failure
        IssueEventListener.behavior = (payload, configFile) -> {
            payload.getIssue().addLabels("otherValue");
        };
        assertThatThrownBy(assertion)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Actual invocations have different arguments:\n" +
                        "GHIssue#750705278.addLabels(\"otherValue\");");
    }

    @Test
    void configFileMocking() {
        ThrowingCallable assertion = () -> given()
                .github(mocks -> {
                    mocks.configFileFromString("config.yml",
                            "someProperty: valueFromConfigFile");
                    Mockito.doNothing()
                            .when(mocks.issue(750705278)).addLabels(any(String.class));
                })
                .when().payloadFromClasspath("/issue-opened.json")
                .event(GHEvent.ISSUES)
                .then().github(mocks -> {
                    verify(mocks.issue(750705278))
                            .addLabels("valueFromConfigFile");
                    verifyNoMoreInteractions(mocks.ghObjects());
                });

        // Success
        IssueEventListener.behavior = (payload, configFile) -> {
            payload.getIssue().addLabels(configFile.someProperty);
        };
        assertThatCode(assertion).doesNotThrowAnyException();

        // Failure
        IssueEventListener.behavior = (payload, configFile) -> {
            payload.getIssue().addLabels("notValueFromConfigFile");
        };
        assertThatThrownBy(assertion)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Argument(s) are different! Wanted:\n" +
                        "GHIssue#750705278.addLabels(\n" +
                        "    \"valueFromConfigFile\"\n" +
                        ");");
    }

    @Test
    void missingMock() {
        IssueEventListener.behavior = (payload, configFile) -> {
            payload.getIssue().getComments().get(0).createReaction(ReactionContent.EYES);
        };
        assertThatThrownBy(() -> when().payloadFromClasspath("/issue-opened.json")
                .event(GHEvent.ISSUES)
                .then().github(mocks -> {
                    verify(mocks.issue(750705278))
                            .addLabels("someValue");
                    verifyNoMoreInteractions(mocks.ghObjects());
                }))
                        .hasMessageContaining(
                                "Mocked behavior is missing for GHIssue#750705278.getComments();."
                                        + " Use the following syntax to mock the behavior of GitHub objects:\n"
                                        + "    given()\n"
                                        + "        .github(mocks -> {\n"
                                        + "            Mockito.when(mocks.ghObject(GHIssue.class, <the ID of the GHObject>).getComments())\n"
                                        + "                    .thenReturn([...]);\n"
                                        + "        })\n"
                                        + "        .when(). [...]");
    }

}
