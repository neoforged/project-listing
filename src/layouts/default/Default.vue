<template>
  <v-app>
    <default-bar/>

    <Suspense>
      <default-view/>

      <template #fallback>
        <v-container class="fill-height">
          <v-responsive class="align-center text-center fill-height">
            <v-progress-circular
                color="primary"
                indeterminate
                size="64"
            />
          </v-responsive>
        </v-container>
      </template>
    </Suspense>

    <v-footer border>
      <div class="px-4 py-2 text-center w-100">
        © 2023 — <a class="noSelect" href="https://github.com/NeoForged"><strong>NeoForged</strong></a>
        <br>
        Contribute on <a class="noSelect" href="https://github.com/NeoForged/project-listing">GitHub</a>
      </div>
    </v-footer>
  </v-app>
</template>

<script lang="ts" setup>
import DefaultBar from './AppBar.vue'
import DefaultView from './View.vue'
</script>
