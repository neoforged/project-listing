<template>
  <v-container class="fill-height" v-if="!error">
    <v-breadcrumbs :items="[project.owner.login, project.name]">
      <template v-slot:divider>
        <v-icon icon="mdi-chevron-right"></v-icon>
      </template>
    </v-breadcrumbs>
    <v-responsive class="text-center fill-height">
      <v-row no-gutters>
        <v-col>
          <div>
            <h2>{{ project.name }}</h2>
            <v-chip
                class="ma-2"
                color="yellow"
                variant="outlined"
                v-if="externalSources.central"
            >
              <v-icon start icon="mdi-check-circle"></v-icon> Available on Maven Central </v-chip>
            <v-chip
                class="ma-2"
                color="cyan"
                variant="outlined"
                v-if="externalSources.gpp"
            >
              <v-icon start icon="mdi-check-circle"></v-icon> Available on the Gradle Plugin Portal </v-chip>
            <div>
              Repository: <a :href="project.url">{{ project.path }}</a>
            </div>
            <div>
              Artifact: <code>{{ project.artifact }}</code>
            </div>
            <div v-if="project.license">
              License: <code>{{ project.license.name }}</code>
            </div>
            <div v-if="project.topics.length > 0">
              Topics:
              <v-chip
                  class="ma-2"
                  color="blue"
                  variant="outlined"
                  v-for="topic in project.topics"
                  :key="topic"
              >{{ topic }}</v-chip>
            </div>
            <div><i>
              {{ project.description }}
            </i></div>
          </div>
        </v-col>

        <v-col cols="12" md="5">
          <v-card title="Recent Versions">
            <template v-slot:append>
              <v-container>
                <v-row>
                  <v-col>
                    <v-tooltip text="Copy to clipboard">
                      <template v-slot:activator="{ props }">
                        <v-btn density="comfortable" icon="mdi-content-copy" :disabled="!selectedVersion" v-bind="props" v-on:click="this.copySelected()" />
                      </template>
                    </v-tooltip>
                  </v-col>
                  <v-col>
                    <v-tooltip text="Download">
                      <template v-slot:activator="{ props }">
                        <v-btn density="comfortable" icon="mdi-download" :disabled="!selectedVersion" v-bind="props" :href="`https://maven.neoforged.net/releases/${mavenPath}/${selectedVersion}/${project.artifact.split(':')[1]}-${selectedVersion}.jar`" />
                      </template>
                    </v-tooltip>
                  </v-col>
                </v-row>
              </v-container>
            </template>
            <v-card-text>
              <v-select id="versionSelect" :items="versions" v-model="selectedVersion" :label="errors.failedVersions ? 'Could not query versions' : 'Version'" density="compact" :disabled="errors.failedVersions">
                <template v-slot:item="{ props, item }">
                  <v-list-item v-bind="props">
                    <template v-slot:append v-if="item.raw == bestLatestVersion || item.raw.endsWith('-SNAPSHOT')">
                      <div class="text-center">
                        <v-chip
                            class="ma-2"
                            color="success"
                            variant="outlined"
                            v-if="item.raw == bestLatestVersion"
                        >
                          <v-icon start icon="mdi-check-circle"></v-icon> Latest
                        </v-chip>
                        <v-chip
                            class="ma-2"
                            color="warning"
                            variant="outlined"
                            v-if="item.raw.endsWith('-SNAPSHOT')"
                        >
                          <v-icon start icon="mdi-alert-octagram"></v-icon> Unstable
                        </v-chip>
                      </div>
                    </template>
                  </v-list-item>
                </template>
              </v-select>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>

      <div class="py-2" />

      <v-divider />

      <div v-html="markdownToHtml" style="text-align: start" />

      <v-divider />
      <div class="py-2" />
      <div class="text-left">
        <v-list lines="one" density="compact">
          <v-list-subheader>LATEST COMMITS</v-list-subheader>
          <v-list-item
              :prepend-avatar="commit.author.avatar_url"
              v-for="commit in commits"
              :key="commit.sha"
              :subtitle="commit.author.login"
          >
            <v-list-item-title>
              <a :href="commit.commit.html_url" class="noSelect">{{ commit.commit.message }}</a>
            </v-list-item-title>
          </v-list-item>
        </v-list>
      </div>

    </v-responsive>
  </v-container>

  <v-container class="fill-height" v-else>
    <v-responsive class="align-center text-center fill-height">
      <h1>Unknown project!</h1>
    </v-responsive>
  </v-container>
</template>

<script lang="ts">
import json from "../../src/repos.json";
import {useRoute} from "vue-router";
import {marked} from "marked";

export default {
  data() {
    return {
      selectedVersion: null
    }
  },

  async setup() {
    const repo = useRoute().params.name as string;
    const org = useRoute().params.org as string;
    const project = json[org.toLowerCase() + '/' + repo.toLowerCase()]

    if (!project) {
      return {
        error: 'Not Found'
      }
    }

    let failedVersions = false

    const proj = await fetch(`https://api.github.com/repos/${org}/${repo}`).then(res => res.json());

    const mavenPath = project.artifact.replace(/\./g, '/').replace(':', '/')

    const versions = await fetch(`https://maven.neoforged.net/api/maven/versions/releases/` + mavenPath)
        .then(response => response.json())
        .then(res => res.versions as string[])
        .then(versions => versions.reverse())
        .catch(err => {
          console.log('Failed to find versions: ' + err)
          failedVersions = true
          return []
        })

    const isOnCentral = await fetch(`https://repo.spongepowered.org/repository/maven-central/${mavenPath}/maven-metadata.xml`) // We use the sponge mirror because central is blocked by CORS
        .then(res => res.status == 200)
        .catch(() => false)

    const isOnGPP = !isOnCentral && await fetch(`https://repo.spongepowered.org/repository/gradleplugins-proxy/${mavenPath}/maven-metadata.xml`) // We use the sponge mirror because GPP is blocked by CORS
        .then(res => res.status == 200)
        .catch(() => false)

    const bestLatestVersion = versions.find(e => !e.endsWith('-SNAPSHOT')) ?? versions[0]

    const readme = await fetch(`https://raw.githubusercontent.com/${org}/${repo}/${project.default_branch}/README.md`).then(res => res.text())
        .then(text => text == '404: Not Found' ? '*No readme available*' : text)
        .catch(() => "*No readme available*")

    const commits = await fetch(`https://api.github.com/repos/${org}/${repo}/commits`).then(res => res.json())
        .then(res => res as object[])
        .then(res => res.slice(0, 10))

    return {
      mavenPath: mavenPath,
      project: {
        owner: proj.owner,
        path: 'neoforged/' + repo,
        url: `https://github.com/${org}/${repo}`,
        artifact: project.artifact,
        name: project.name,
        description: proj.description,
        topics: proj.topics ? proj.topics : [],
        readme: readme,
        license: proj.license
      },
      bestLatestVersion: bestLatestVersion,
      versions: versions,
      commits: commits,

      errors: {
        failedVersions: failedVersions
      },

      error: null,

      externalSources: {
        central: isOnCentral,
        gpp: isOnGPP
      }
    };
  },
  computed: {
    markdownToHtml() {
      return marked(this.project.readme)
    }
  },
  methods: {
    copySelected() {
      navigator.clipboard.writeText(this.selectedVersion)
    }
  }
}
</script>

<style scoped>
.noSelect {
  -webkit-tap-highlight-color: transparent;
  -webkit-touch-callout: none;
  -webkit-user-select: none;
  -khtml-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
  user-select: none;
  color: inherit;
  text-decoration: inherit;
}
.noSelect:focus {
  outline: none !important;
}
</style>
