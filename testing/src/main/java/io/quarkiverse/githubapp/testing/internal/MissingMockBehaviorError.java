package io.quarkiverse.githubapp.testing.internal;

import org.mockito.internal.util.MockUtil;
import org.mockito.invocation.InvocationOnMock;

public class MissingMockBehaviorError extends AssertionError {
    public InvocationOnMock invocation;

    public MissingMockBehaviorError(InvocationOnMock invocation) {
        super(buildMessage(invocation));
        this.invocation = invocation;
    }

    private static String buildMessage(InvocationOnMock invocation) {
        String methodCall = toMethodCall(invocation);
        return "Mocked behavior is missing for " + invocation.toString()
                + ". Use the following syntax to mock the behavior of GitHub objects:\n"
                + "    given()\n"
                + "        .github(mocks -> {\n"
                + "            Mockito.when(mocks.ghObject(" + toClassName(invocation)
                + ".class, <the ID of the GHObject>)." + methodCall + ")\n"
                + "                    .thenReturn([...]);\n"
                + "        })\n"
                + "        .when(). [...]";
    }

    private static String toClassName(InvocationOnMock invocation) {
        try {
            return invocation.getMock().getClass().getSuperclass().getSimpleName();
        } catch (Throwable t) {
            // The code above is a bit fragile, so we'll fall back to this if it doesn't work
            return "<the GHObject class>";
        }
    }

    private static String toMethodCall(InvocationOnMock invocation) {
        try {
            String mockNameAndInvocationAndSemicolon = invocation.toString();
            String mockName = MockUtil.getMockName(invocation.getMock()).toString();
            return mockNameAndInvocationAndSemicolon.substring(
                    mockName.length() + 1, // Remove mock name and dot
                    mockNameAndInvocationAndSemicolon.length() - 1 // Remove semicolon
            );
        } catch (Throwable t) {
            // The code above is a bit fragile, so we'll fall back to this if it doesn't work
            return invocation.getMethod().getName() + "(...)";
        }
    }

}
