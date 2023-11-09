<template>
  <v-chip
      class="ma-2"
      color="yellow"
      variant="outlined"
  >
    {{ amount }}
    <v-icon end icon="mdi-star-outline"/>
  </v-chip>
  <v-chip
      class="ma-2"
      color="grey"
      variant="outlined"
      v-if="isFork"
  >
    <v-icon icon="mdi-source-fork"/>
  </v-chip>
</template>

<script lang="ts">
import {getRepoInfo} from "@/scripts/githubAPI";

export default {
  async setup(params) {
    const response = await getRepoInfo(params.repo!)
    return {
      amount: response.stargazers_count as number,
      isFork: response.fork as boolean
    }
  },
  props: {
    repo: String
  }
}
</script>
