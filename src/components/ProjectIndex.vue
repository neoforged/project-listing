<template>
  <v-container class="fill-height">
    <v-responsive class="align-center fill-height">
      <v-img height="150" src="https://github.com/Neoforged.png"/>
      <h1 class="text-center">NeoForged Project Index</h1>

      <div class="py-6"/>

      <v-row class="d-flex align-center justify-center">
        <v-col v-for="repo in Object.keys(data)" :key="repo" cols="12" sm="5">
          <v-card class="mx-auto" :title="data[repo].name" :href="`/${repo}`">
            <template v-slot:append>
              <Suspense>
                <RepoChips :repo="repo" />
              </Suspense>
            </template>
            <v-card-item>
              <Suspense>
                <RepoDescription :repo="repo" />
              </Suspense>
            </v-card-item>
            <v-card-actions>
              <v-btn :href="`/${repo}`">Details</v-btn>
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

export default {
  components: {RepoChips, RepoDescription},
  data() {
    return {
      data: json
    }
  },
};
</script>
