import groovy.json.JsonGenerator
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import java.util.stream.Stream

@GrabResolver(name = 'central', root='https://repo1.maven.org/maven2/')
@Grapes([
        @Grab('org.apache.groovy:groovy-json:4.0.14'),
        @Grab('org.kohsuke:github-api:1.313')
])

final GitHub gh = new GitHubBuilder()
        .withJwtToken(System.getenv('AUTH_TOKEN'))
        .build()

class CustomProperty {
    String property_name, value
}

static CustomProperty[] getCustomProperties(GHRepository repo) {
    repo.root().createRequest().withUrlPath('/repos/' + repo.fullName + '/properties/values').fetch(CustomProperty[])
}

static String findProperty(CustomProperty[] properties, String name) {
    final prop = properties.find({ it.property_name == name })?.value
    if (prop?.startsWith('gist:')) {
        final parts = prop.replace('gist:', '').split('/')
        final actualUrl = "https://gist.githubusercontent.com/${parts[0]}/${parts[1]}/raw/${parts[2]}"
        return new String(URI.create(actualUrl).toURL().openStream().readAllBytes(), StandardCharsets.UTF_8)
    }
    return null
}

final repos = [:]

gh.getOrganization('neoforged').repositories.forEach { name, repo ->
    final properties = getCustomProperties(repo)
    final artifact = findProperty(properties, 'ArtifactID')
    if (artifact) {
        repos[repo.fullName.toLowerCase(Locale.ROOT)] = [
                artifact: artifact,
                name: repo.name,
                default_branch: repo.defaultBranch,
                version_pattern: findProperty(properties, 'ProjectListing_VersionPattern'),
                version_display_pattern: findProperty(properties, 'ProjectListing_VersionDisplayPattern')?.split(',')
                    ?.toList()?.stream()?.map { it.split(':') }?.collect(Collectors.toMap({ it[0].trim() }, { it[1].trim() })) ?: [:],
                download_url_pattern: findProperty(properties, 'ProjectListing_DownloadURLPattern')
        ]
        println("Found repository ${repo.fullName} with declared artifact ID: $artifact")
    }
}

final outPath = Path.of('src/repos.json')
final asString = new JsonGenerator.Options().build().toJson(repos)
if (Files.readString(outPath).trim() != asString.trim()) {
    Files.writeString(outPath, asString)
    println("JSON file updated; running git commands")
    new ProcessBuilder('git', 'add', 'src/repos.json')
        .inheritIO().start().waitFor()
    new ProcessBuilder('git', 'commit', '-m', 'Update repos json')
        .inheritIO().start().waitFor()
    new ProcessBuilder('git', 'push')
        .inheritIO().start().waitFor()
}