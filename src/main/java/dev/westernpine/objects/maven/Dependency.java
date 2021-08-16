package dev.westernpine.objects.maven;

import java.net.MalformedURLException;
import java.net.URL;

public class Dependency {
	
	public final String groupId;
	public final String artifactId;
	public final String version;
	public Repository repository = Repository.MAVEN_CENTRAL_REPOSITORY;
	
	public Dependency(String groupId, String artifactId, String version) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
	}
	
	public Dependency withRepository(Repository repository) {
		this.repository = repository;
		return this;
	}
	
	public URL getUrl() throws MalformedURLException {
        String repo = this.repository.url;
        if (!repo.endsWith("/")) {
            repo += "/";
        }
        repo += "%s/%s/%s/%s-%s.jar";
        String url = String.format(repo, this.groupId.replace(".", "/"), this.artifactId, this.version, this.artifactId, this.version);
        return new URL(url);
    }
	
    @Override
    public String toString() {
        return "LibraryLoader.Dependency(" +
                "groupId=" + this.groupId + ", " +
                "artifactId=" + this.artifactId + ", " +
                "version=" + this.version + ", " +
                "repoUrl=" + (this.repository == null ? this.repository.url : "null") + ")";
    }

}
