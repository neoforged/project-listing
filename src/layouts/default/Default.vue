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
  </v-app>
</template>

<script lang="ts" setup>
import DefaultBar from './AppBar.vue'
import DefaultView from './View.vue'
</script>
