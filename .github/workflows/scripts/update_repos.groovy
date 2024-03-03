import com.fasterxml.jackson.databind.JsonNode
import groovy.json.JsonGenerator
import groovy.json.JsonSlurper
import groovy.transform.ImmutableOptions
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import java.util.stream.StreamSupport

@GrabResolver(name = 'central', root='https://repo1.maven.org/maven2/')
@Grapes([
        @Grab('org.apache.groovy:groovy-json:4.0.14'),
        @Grab('org.kohsuke:github-api:1.313'),
        @Grab('info.picocli:picocli:4.7.5')
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
    return prop
}

static JsonNode graphQl(GitHub gitHub, String query) throws IOException {
    return gitHub.createRequest()
            .method("POST")
            .inBody()
            .with("query", query)
            .withUrlPath("/graphql")
            .fetch(JsonNode.class);
}

final pinned = StreamSupport.stream(graphQl(gh, """
{
  organization(login: "neoforged") {
    pinnedItems(first: 6, types: REPOSITORY) {
      nodes {
        ... on Repository {
          name
        }
      }
    }
  }
}""").get('data').get('organization').get('pinnedItems').get('nodes').spliterator(), false)
    .map { it.get('name').textValue() }.toList()

@ImmutableOptions(knownImmutableClasses = Map)
record RepoInfo(String name, String fullName, Map info) {}

final repos = [] as List<RepoInfo>

gh.getOrganization('neoforged').repositories.forEach { name, repo ->
    final properties = getCustomProperties(repo)
    final artifact = findProperty(properties, 'ArtifactID')
    if (artifact) {
        repos.add(new RepoInfo(
                repo.name,
                repo.fullName.toLowerCase(Locale.ROOT),
                [
                        artifact: artifact,
                        name: repo.name,
                        default_branch: repo.defaultBranch,
                        version_pattern: findProperty(properties, 'ProjectListing_VersionPattern'),
                        version_display_pattern: findProperty(properties, 'ProjectListing_VersionDisplayPattern')?.split(',')
                                ?.toList()?.stream()?.map { it.split(':') }?.collect(Collectors.toMap({ it[0].trim() }, { it[1].trim() })) ?: [:],
                        download_url_pattern: findProperty(properties, 'ProjectListing_DownloadURLPattern')
                ]
        ))
        println("Found repository ${repo.fullName} with declared artifact ID: $artifact")
    }
}

final repoMap = [:]

pinned.forEach { pin ->
    repos.find { it.name() == pin }?.tap {
        repoMap[it.fullName()] = it.info()
        repos.remove(it)
    }
}
repos.sort {
    it.name() <=> it.name()
}
repos.forEach { repo ->
    repoMap[repo.fullName()] = repo.info()
}

final outPath = Path.of('src/repos.json')
final asString = new JsonGenerator.Options().build().toJson(repoMap)
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

final readJson = { String uri ->
    final connection = URI.create(uri).toURL().openConnection()
    connection.addRequestProperty('Accept', 'application/json')
    connection.addRequestProperty('Authorization', gh.client.authorizationProvider.encodedAuthorization)
    connection.connect()
    connection.getInputStream().withCloseable {
        new JsonSlurper().parse(it.readAllBytes())
    }
}

final repoInfo = [:]
repoMap.keySet().forEach { key ->
    repoInfo[key] = [
        info: readJson("https://api.github.com/repos/${key}"),
        commits: readJson("https://api.github.com/repos/${key}/commits?per_page=10")
    ]
}
Files.writeString(Path.of('src/repoInfo.json'), new JsonGenerator.Options().build().toJson(repoInfo))
