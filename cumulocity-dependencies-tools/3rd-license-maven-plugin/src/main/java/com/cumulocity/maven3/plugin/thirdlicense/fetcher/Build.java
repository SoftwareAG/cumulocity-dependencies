package com.cumulocity.maven3.plugin.thirdlicense.fetcher;

import com.google.inject.internal.util.Sets;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Set;

@Getter
@Setter
@ToString
public class Build {

    private String revision;

    private Set<FetcherDependency> dependencies = Sets.newHashSet();
}
