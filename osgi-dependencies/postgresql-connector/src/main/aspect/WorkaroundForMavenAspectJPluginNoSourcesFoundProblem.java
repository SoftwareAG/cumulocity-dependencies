/**
 * When there are no sources in the project, the aspectj-maven-plugin skips it's execution, 
 * despite it's told to weave the compiled classes. We cannot use the original postgresql
 * sources, because the do not compile, as they contain JDBC-3 implementation.
 * @author Darek Kaczynski
 */
public final class WorkaroundForMavenAspectJPluginNoSourcesFoundProblem {

    private WorkaroundForMavenAspectJPluginNoSourcesFoundProblem() {
    }
}
