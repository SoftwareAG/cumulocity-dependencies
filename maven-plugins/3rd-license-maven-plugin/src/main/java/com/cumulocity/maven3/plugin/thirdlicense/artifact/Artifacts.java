package com.cumulocity.maven3.plugin.thirdlicense.artifact;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Artifacts {

    public static final boolean isCumulocityArtifact(String groupId) {
        return isCumulocityInternalDependency(groupId)
                || isCumulocityAgentInternalDependency(groupId);
    }

    public static final boolean isCumulocityAgentInternalDependency(String groupId) {
        return groupId != null
                && (groupId.startsWith("c8y.agents")
                || groupId.startsWith("c8y-agents"));
    }

    public static final boolean isCumulocityInternalDependency(String groupId) {
        return groupId != null
                && groupId.startsWith("com.nsn.cumulocity")
                && !groupId.startsWith("com.nsn.cumulocity.dependencies.osgi");
    }

    public static final boolean isThirdPartyRepackedArtifact(String groupId) {
        return groupId != null && groupId.startsWith("com.nsn.cumulocity.dependencies.osgi");
    }
}
