/**
 * main.ts
 *
 * Bootstraps Vuetify and other plugins then mounts the App`
 */

// Components
import App from './App.vue'

// Composables
import { createApp } from 'vue'

// Plugins
import { registerPlugins } from '@/plugins'
import VersionSelect from "@/components/VersionSelect.vue";
import Unstable from "@/components/chips/Unstable.vue";
import Latest from "@/components/chips/Latest.vue";

const app = createApp(App)

registerPlugins(app)

app.component('VersionSelect', VersionSelect)
app.component('UnstableChip', Unstable)
app.component('LatestChip', Latest)

app.mount('#app')
