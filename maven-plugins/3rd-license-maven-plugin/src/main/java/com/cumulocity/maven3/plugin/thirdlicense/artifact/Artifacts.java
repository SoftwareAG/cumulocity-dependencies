package com.cumulocity.maven3.plugin.thirdlicense.artifact;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Artifacts {

    public static boolean isCumulocityArtifact(String groupId) {
        return isCumulocityInternalDependency(groupId)
                || isCumulocityAgentInternalDependency(groupId);
    }

    public static boolean isCumulocityAgentInternalDependency(String groupId) {
        return groupId != null
                && (groupId.startsWith("c8y.agents")
                || groupId.startsWith("c8y-agents"));
    }

    public static boolean isCumulocityInternalDependency(String groupId) {
        return groupId != null
                && groupId.startsWith("com.nsn.cumulocity")
                && !groupId.startsWith("com.nsn.cumulocity.dependencies.osgi");
    }

    public static boolean isThirdPartyRepackedArtifact(String groupId) {
        return groupId != null && groupId.startsWith("com.nsn.cumulocity.dependencies.osgi");
    }
}
