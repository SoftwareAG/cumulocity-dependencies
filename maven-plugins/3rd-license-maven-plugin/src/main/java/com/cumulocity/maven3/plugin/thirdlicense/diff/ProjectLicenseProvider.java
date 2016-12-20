package com.cumulocity.maven3.plugin.thirdlicense.diff;

import java.io.File;

public interface ProjectLicenseProvider {
    
    File get(String version);

}
