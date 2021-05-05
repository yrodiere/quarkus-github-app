package io.quarkiverse.githubapp.testing.internal;

import static org.mockito.Mockito.withSettings;

import java.io.Serializable;

import org.kohsuke.github.GHObject;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

final class CallRealMethodAndSpyGHObjectResults implements Answer<Object>, Serializable {

    private final GitHubMockContextImpl mocks;

    public CallRealMethodAndSpyGHObjectResults(GitHubMockContextImpl mocks) {
        this.mocks = mocks;
    }

    @Override
    public Object answer(InvocationOnMock invocation) throws Throwable {
        Object original;
        try {
            original = invocation.callRealMethod();
        } catch (MissingMockBehaviorError error) {
            // Calling the real method resulted in a mock error?
            // That means the real method calls another method on a mock.
            // Users really should mock the (calling) real method.
            throw new MissingMockBehaviorError(invocation);
        }
        if (!(original instanceof GHObject)) {
            return original;
        }
        GHObject castOriginal = (GHObject) original;
        Class<? extends GHObject> type = castOriginal.getClass();
        DefaultableMocking<? extends GHObject> mocking = mocks.ghObjectMocking(castOriginal);
        return Mockito.mock(type, withSettings().stubOnly()
                .name(mocking.name())
                .withoutAnnotations()
                .spiedInstance(original)
                .defaultAnswer(new CallMockedMethodOrCallRealMethodAndSpyGHObjectResults(this, mocking)));
    }

}
