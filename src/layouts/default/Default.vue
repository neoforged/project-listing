<template>
  <v-app>
    <default-bar />

    <Suspense>
      <default-view />

      <template #fallback>
        <v-overlay
            :model-value="true"
            class="align-center justify-center"
        >
          <v-progress-circular
              color="primary"
              indeterminate
              size="64"
          ></v-progress-circular>
        </v-overlay>
      </template>
    </Suspense>
  </v-app>
</template>

<script lang="ts" setup>
  import DefaultBar from './AppBar.vue'
  import DefaultView from './View.vue'
</script>
