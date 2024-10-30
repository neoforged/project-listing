<template>
  <v-container class="fill-height">
    <v-responsive class="align-center fill-height">
      <v-img height="150" src="https://github.com/Neoforged.png" />
      <h1 class="text-center">NeoForged Project Index</h1>

      <div class="py-6" />

      <v-row class="d-flex align-center justify-center">
        <v-col v-for="repo in repos" :key="repo.projectRepo" cols="12" sm="5">
          <v-card class="mx-auto" :title="repo.name" :href="`/${repo.projectRepo}`">
            <template v-slot:append>
              <Suspense>
                <RepoChips :repo="repo.projectRepo" />
              </Suspense>
            </template>

            <v-card-item>
              <Suspense>
                <RepoDescription :repo="repo.projectRepo" />
              </Suspense>
            </v-card-item>

            <v-card-actions>
              <v-btn :href="`/${repo.projectRepo}`">Details</v-btn>
              <v-spacer></v-spacer>
              <v-btn :href="`/${repo.projectRepo}`">{{ repo.latestVersion || 'N/A' }}</v-btn>
            </v-card-actions>
          </v-card>
        </v-col>
      </v-row>
    </v-responsive>
  </v-container>
</template>

<script lang="ts">
import json from "../../src/repos.json";
import RepoChips from "@/components/project/RepoChips.vue";
import RepoDescription from "@/components/project/RepoDescription.vue";
import { ref, onMounted } from "vue";

export default {
  components: {RepoChips, RepoDescription},
  async setup() {
    const repos = ref([])

    const fetchRepoData = async () => {
      const repoDataPromises = Object.keys(json).map(async (project) => {
        const projectInfo = json[project];
        const mavenPath = projectInfo.artifact.replace(/\./g, '/').replace(':', '/');

        const versions = await fetch(`https://maven.neoforged.net/api/maven/versions/releases/${mavenPath}`)
          .then((res) => res.json())
          .then((res) => res.versions.reverse())
          .catch((err) => {
            console.error("Failed to fetch versions:", err);
            return [];
          });

        return {
          ...projectInfo,
          projectRepo: project,
          latestVersion: versions[0] || 'N/A',
        };
      });

      repos.value = await Promise.all(repoDataPromises);
    };

    onMounted(fetchRepoData);

    return {
      repos
    }
  }
};
</script>
