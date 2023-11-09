<template>
  <i>{{ description }}</i>
</template>

<script lang="ts">
import {getRepoInfo} from "@/scripts/githubAPI";
export default {
  async setup(params) {
    const response = await getRepoInfo(params.repo!)
        .then(res => res.description ? res.description : 'Repository has no description')
        .catch(err => {
          console.log("Failed to find query repo description: " + err)
          return "Description could not be fetched"
        })
    return {
      description: response
    }
  },
  props: {
    repo: String
  }
}
</script>
