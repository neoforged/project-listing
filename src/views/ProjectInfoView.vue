<template>
  <v-container class="fill-height" v-if="!error">
    <v-responsive class="text-center fill-height">
      <v-breadcrumbs :items="[{title: project!.owner.login, disabled: false, href: '/'}, project!.name]">
        <template v-slot:divider>
          <v-icon icon="mdi-chevron-right"></v-icon>
        </template>
      </v-breadcrumbs>
      <v-row no-gutters>
        <v-col align-self="center">
          <div>
            <h2>{{ project!.name }}</h2>
            <v-chip
                class="ma-2"
                color="yellow"
                variant="outlined"
                v-if="externalSources!.central"
            >
              <v-icon start icon="mdi-check-circle"></v-icon>
              Available on Maven Central
            </v-chip>
            <v-chip
                class="ma-2"
                color="cyan"
                variant="outlined"
                v-if="externalSources!.gpp"
            >
              <v-icon start icon="mdi-check-circle"></v-icon>
              Available on the Gradle Plugin Portal
            </v-chip>
            <div>
              Repository: <a :href="project!.url">{{ project!.path }}</a>
            </div>
            <div>
              Artifact: <code>{{ project!.artifact }}</code>
            </div>
            <div v-if="project!.license">
              License: <code>{{ project!.license.name }}</code>
            </div>
            <div v-if="project!.topics.length > 0">
              Topics:
              <v-chip
                  class="ma-2"
                  color="blue"
                  variant="outlined"
                  v-for="topic in project!.topics"
                  :key="topic"
              >{{ topic }}
              </v-chip>
            </div>
            <div><i>
              {{ project!.description }}
            </i></div>
          </div>
        </v-col>

        <v-col cols="12" md="5">
          <v-img v-if="getProjectLogo" width="100%" height="100px" :src="getProjectLogo"/>
          <v-card title="Select Version">
            <template v-slot:append>
              <v-container>
                <v-row dense>
                  <v-col>
                    <v-tooltip text="Download">
                      <template v-slot:activator="{ props }">
                        <v-btn density="comfortable" icon="mdi-download" :disabled="!selectedVersion" v-bind="props"
                               :href="computeDownloadUrl()"/>
                      </template>
                    </v-tooltip>
                  </v-col>
                  <v-col>
                    <v-tooltip text="Copy version to clipboard">
                      <template v-slot:activator="{ props }">
                        <v-btn density="comfortable" icon="mdi-content-copy" :disabled="!selectedVersion" v-bind="props"
                               v-on:click="copySelected()"/>
                      </template>
                    </v-tooltip>
                  </v-col>
                  <v-col>
                    <v-tooltip text="Changelog">
                      <template v-slot:activator="{ props }">
                        <v-btn density="comfortable" icon="mdi-clipboard-text-clock-outline" :disabled="!displayChangelog"
                               v-bind="props" :href="computeChangelogUrl(selectedVersion)" target="_blank"/>
                      </template>
                    </v-tooltip>
                  </v-col>
                </v-row>
              </v-container>
            </template>
            <v-card-text>
              <VersionSelect :versions="versions" v-model="selectedVersion"
                             :pattern="versionPattern"
                             :display-pattern="versionDisplayPattern"
                             @update:modelValue="updateVersion($event)"
                             :errors="errors!.failedVersions"/>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>

      <div class="py-2"/>

      <v-divider/>

      <div class="markdown-body" v-html="markdownToHtml" style="text-align: start"/>

      <v-divider/>
      <div class="py-2"/>
      <div class="text-left">
        <v-list lines="one" density="compact">
          <v-list-subheader>LATEST COMMITS</v-list-subheader>
          <v-list-item
              :prepend-avatar="commit.author?.avatar_url ?? 'https://github.com/ghost.png'"
              v-for="commit in commits"
              :key="commit.sha"
              :subtitle="getAuthorName(commit)"
          >
            <v-list-item-title>
              <a :href="commit.html_url" class="noSelect">{{ commit.commit.message }}</a>
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
import {marked} from "@/main";
import VersionSelect from "@/components/VersionSelect.vue";
import {Commit, getRepoCommits, getRepoInfo, License, User} from "@/scripts/githubAPI";

interface Project {
  owner: User
  defaultBranch: string
  path: string
  url: string
  artifact: string
  artifactName: string
  name: string
  description: string
  topics: string[]
  license: License
  readme: {
    readme: string
    dir: string
  }
}

interface ExternalSources {
  central: boolean
  gpp: boolean
}

interface ComponentData {
  mavenPath?: string
  downloadUrlPattern?: string
  project?: Project

  error: string | null
  errors?: {
    failedVersions: boolean
  }
  htmlToRender: string
  logoImage: string
  versions?: string[]
  versionPattern?: string
  versionDisplayPattern?: string
  commits?: Commit[]

  externalSources?: ExternalSources
}

export default {
  components: {VersionSelect},
  data() {
    return {
      selectedVersion: (null as unknown as string),
      displayChangelog: false
    }
  },

  async setup() {
    const repo = useRoute().params.name as string;
    const org = useRoute().params.org as string;
    const project = (json as any)[org.toLowerCase() + '/' + repo.toLowerCase()] as any

    if (!project) {
      return {
        error: 'Not Found'
      } as ComponentData
    }

    let failedVersions = false

    const proj = await getRepoInfo(`${org}/${repo}`);

    const mavenPath = project.artifact.replace(/\./g, '/').replace(':', '/')

    const projectPath =  'neoforged/' + repo;

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

    const readme = await fetch(`https://raw.githubusercontent.com/${org}/${repo}/${project.default_branch}/README.md`)
        .then(res => res.text())
        .then(txt => {
          return {
            readme: txt,
            dir: ''
          }
        })
        .then(text => text.readme == '404: Not Found' ? fetch(`https://raw.githubusercontent.com/${org}/${repo}/${project.default_branch}/docs/README.md`).then(res => res.text()).then(res => {
          return {
            readme: res,
            dir: 'docs/'
          }
        }) : text)
        .then(text => text.readme == '404: Not Found' ? {
          readme: '*No readme available*',
          dir: ''
        } : text)
        .catch(() => {
          return {
            readme: "*No readme available*",
            dir: ''
          }
        })

    const commits = await getRepoCommits(`${org}/${repo}`)
        .then(res => res.slice(0, 10))

    // Special handling to remove the main logo from the readme html because it was destracting users away from the download dropdowns. Too much visual noise at bottom of page. 
    const elementToRemoveRegex = /<p><img src="https:\/\/github\.com\/[\w/.]+\/\w*logo\w*\.(png|svg)" alt="[\w ]+">\<\/p>/i
    const readmeContentPromise = marked(`https://github.com/${projectPath}/raw/${project.default_branch}/${readme.dir}`).parse(readme.readme)
    let readmeContent = typeof readmeContentPromise === "string" ? readmeContentPromise : await readmeContentPromise
    const extractedLogoElement = (readmeContent.match(elementToRemoveRegex) ?? [""])[0]
    const extractedLogo = (extractedLogoElement.match(/https:\/\/github\.com\/[\w/.]+\/\w*logo\w*\.(png|svg)/i) ?? [""])[0]
    readmeContent = readmeContent.replace(extractedLogoElement, "");

    return {
      mavenPath: mavenPath,
      downloadUrlPattern: project.download_url_pattern ?? '$maven/$mavenPath/$version/$artifactName-$version.jar',
      project: {
        owner: proj.owner,
        defaultBranch: project.default_branch,
        path: projectPath,
        url: `https://github.com/${org}/${repo}`,
        artifact: project.artifact as string,
        artifactName: project.artifact.split(':')[1],
        name: project.name,
        description: proj.description,
        topics: proj.topics ? proj.topics : [],
        readme: readme,
        license: proj.license
      },
      versions: versions,
      versionPattern: project.version_pattern ?? '(?<Version>.+)',
      versionDisplayPattern: project.version_display_pattern ?? {},
      commits: commits,
      htmlToRender: readmeContent,
      logoImage: extractedLogo,

      errors: {
        failedVersions: failedVersions
      },

      externalSources: {
        central: isOnCentral,
        gpp: isOnGPP
      },

      error: null
    } as ComponentData;
  },
  computed: {
    markdownToHtml() {
      return this.htmlToRender;
    },
    getProjectLogo() {
      return this.logoImage;
    }
  },
  methods: {
    computeDownloadUrl() {
      return this.downloadUrlPattern!.replace('$mavenPath', this.mavenPath!).replace('$maven', 'https://maven.neoforged.net/releases')
          .replaceAll('$version', this.selectedVersion).replace('$artifactName', this.project!.artifactName)
    },
    computeChangelogUrl(version: string): string | undefined {
      if (!version) {
        return undefined;
      }
      return `https://maven.neoforged.net/releases/${this.mavenPath}/${version}/${this.project!.artifactName}-${version}-changelog.txt`;
    },

    updateVersion(version: string) {
      if (version) {
        fetch(this.computeChangelogUrl(version)!)
            .then(res => {
              this.displayChangelog = res.status == 200;
            })
            .catch(err => this.displayChangelog = false);
      } else {
        this.displayChangelog = false;
      }
    },

    copySelected() {
      navigator.clipboard.writeText(this.selectedVersion)
    },
    getAuthorName(commit: any) {
      if (commit.author) {
        return commit.author.login
      }
      const author = commit.author?.login ?? commit.commit.author.name
      return author + (commit.commit.author.name != commit.commit.committer?.name ? ' & ' + commit.commit.committer.name : '')
    }
  }
}
</script>
