package com.cumulocity.maven3.plugin.thirdlicense.fetcher;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class FetcherDependency {

    @Setter(AccessLevel.PRIVATE)
    private String type = "MVN";
    private String name;
    private String version;
}
