<template>
  <i>{{ description }}</i>
</template>

<script lang="ts">
export default {
  async setup(params) {
    const response = await fetch(`https://api.github.com/repos/${params.repo}`).then(res => res.json())
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
