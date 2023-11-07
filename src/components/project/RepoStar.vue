<template>
  <v-chip
      class="ma-2"
      color="yellow"
      variant="outlined"
  >
    {{ amount }}
    <v-icon end icon="mdi-star-outline"/>
  </v-chip>
</template>

<script lang="ts">
export default {
  async setup(params) {
    const response = await fetch(`https://api.github.com/repos/${params.repo}/stargazers`).then(res => res.json())
    return {
      amount: (response as string[]).length
    }
  },
  props: {
    repo: String
  }
}
</script>
