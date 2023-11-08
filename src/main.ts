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
import {Marked} from "marked";
import {markedHighlight} from "marked-highlight";
import * as hljs from "highlight.js";

import 'highlight.js/styles/github-dark.css';
import {baseUrl} from "marked-base-url";

const app = createApp(App)

registerPlugins(app)

app.component('VersionSelect', VersionSelect)
app.component('UnstableChip', Unstable)
app.component('LatestChip', Latest)

app.mount('#app')

export const marked = (baseLinkUrl: string) => new Marked(
    {
        gfm: true
    },
    baseUrl(baseLinkUrl),
    markedHighlight({
        langPrefix: 'hljs language-',
        highlight(code, lang) {
            const language = hljs.default.getLanguage(lang) ? lang : 'plaintext';
            return hljs.default.highlight(code, { language }).value;
        }
    })
);
