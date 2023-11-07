import groovy.json.JsonGenerator
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder

import java.nio.file.Files
import java.nio.file.Path
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

final repos = [:]

gh.getOrganization('neoforged').repositories.forEach { name, repo ->
    final properties = getCustomProperties(repo)
    final artifact = Stream.of(properties).filter { it.property_name == 'ArtifactID' }.findFirst().orElse(null)?.value
    if (artifact) {
        repos[repo.fullName.toLowerCase(Locale.ROOT)] = [
                artifact: artifact,
                name: repo.name,
                default_branch: repo.defaultBranch
        ]
        println("Found repository ${repo.fullName} with declared artifact ID: $artifact")
    }
}

final outPath = Path.of('src/repos.json')
final asString = new JsonGenerator.Options().build().toJson(repos)
if (Files.readString(outPath).trim() != asString.trim()) {
    Files.writeString(outPath, asString)
    println("JSON file updated; running git commands")
    Runtime.getRuntime().exec(new String[] { 'git', 'add', 'src/repos.json' }).waitFor()
    Runtime.getRuntime().exec(new String[] { 'git', 'commit', '-M', 'Update repos json' }).waitFor()
    Runtime.getRuntime().exec(new String[] { 'git', 'push' }).waitFor()
}