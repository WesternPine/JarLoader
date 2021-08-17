package dev.westernpine.objects.maven;

import java.net.MalformedURLException;
import java.net.URL;

public class Dependency {
	
	/**
	 * The dependency Group ID.
	 */
	public final String groupId;
	
	/**
	 * The dependency Artifact ID.
	 */
	public final String artifactId;
	
	/**
	 * The dependency Version.
	 */
	public final String version;
	
	/**
	 * The repository to download the dependency from. (Default: Maven Central)
	 */
	public Repository repository = Repository.MAVEN_CENTRAL_REPOSITORY;
	
	/**
	 * A new Dependency object representing required code. Note that not all dependencies are UBER Jars, so you may need to add additional dependencies.
	 * @param groupId The groupId of the dependency.
	 * @param artifactId The artifactId of the dependency.
	 * @param version The version of the dependency.
	 */
	public Dependency(String groupId, String artifactId, String version) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
	}
	
	/**
	 * Set the repository for this dependency. (Default: Maven Central)
	 * @param repository The Repository to download this dependency from.
	 * @return The Dependency object, instended to be used functionally.
	 */
	public Dependency withRepository(Repository repository) {
		this.repository = repository;
		return this;
	}
	
	/**
	 * Get the URL of this dependency in coordination with the set repository.
	 * @return The URL of this dependency in coordination with the set repository.
	 * @throws MalformedURLException If the URL is invalid.
	 */
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
