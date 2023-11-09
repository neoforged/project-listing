<template>
  <v-select v-if="errors" :items="[]" disabled="true" label="Could not query versions"/>
  <template v-for="(group, index) in topLevelGroups" :key="index">
    <v-select :items="possibleItems[index]" :item-value="item => item"
              :item-title="item => versionDisplay(item, group, index)"
              :label="group.replace('_', ' ')"
              v-model="selectedItem[index]" density="compact"
              :disabled="!isEnabled(index)" @update:modelValue="onUpdate(index, $event)">
      <template v-slot:append-inner
                v-if="selectedItem[index] == bestLatestVersions[index] && bestLatestVersions[index] != null || selectedItem[index]?.endsWith('-SNAPSHOT')">
        <div class="text-center">
          <LatestChip v-if="selectedItem[index] == bestLatestVersions[index]"/>
          <UnstableChip v-if="selectedItem[index]?.endsWith('-SNAPSHOT')"/>
        </div>
      </template>
      <template v-slot:item="{ props, item }">
        <v-list-item v-bind="props">
          <template v-slot:append v-if="item.raw == bestLatestVersions[index] || item.raw.endsWith('-SNAPSHOT')">
            <div class="text-center">
              <LatestChip v-if="item.raw == bestLatestVersions[index]"/>
              <UnstableChip v-if="item.raw.endsWith('-SNAPSHOT')"/>
            </div>
          </template>
        </v-list-item>
      </template>
    </v-select>
  </template>
</template>

<script lang="ts">
import {reactive} from "vue";

type Value = {
  name: string,
  children: Map<string, Value>
}

class Reactive extends Array<string> {
  i: boolean = false;

  update() {
    this.i = !this.i;
  }
}

function calculateBestLatest(versions: string[]): string {
  return versions.find(e => !e.endsWith('-SNAPSHOT')) ?? (versions.length == 0 ? undefined : versions[0])
}

export default {
  setup(params) {
    const pattern = new RegExp(params.pattern)
    const versionTree: Value = {
      name: '',
      children: new Map()
    }
    const topLevelGroups = new Array<string>();
    (params.versions as string[]).forEach(version => {
      const result = (pattern.exec(version) as RegExpExecArray);
      if (result === null) return;
      const groups = result.groups
      let currentLevel = versionTree
      for (let groupKey in groups) {
        if (!topLevelGroups.includes(groupKey)) topLevelGroups.push(groupKey)
        let existing = currentLevel.children.get(groups[groupKey])
        if (existing === undefined) {
          existing = {
            name: groups[groupKey],
            children: new Map()
          }
          currentLevel.children.set(groups[groupKey], existing)
        }
        currentLevel = existing
      }
      currentLevel.children.set('full', {
        name: version,
        children: null
      })
    })

    const possibleItems = new Array<Reactive>(topLevelGroups.length)
    for (let i = 0; i < topLevelGroups.length; i++) possibleItems[i] = reactive(new Reactive());
    for (let key of versionTree.children.keys()) {
      possibleItems[0].push(key)
    }

    const bestLatestVersions = new Array<string>(topLevelGroups.length);
    bestLatestVersions[0] = calculateBestLatest(possibleItems[0])

    return {
      topLevelGroups,
      versionTree,
      possibleItems,
      bestLatestVersions,
      selectedItem: reactive(new Reactive(topLevelGroups.length))
    }
  },
  methods: {
    isEnabled(index: number) {
      for (let i = 0; i < index; i++) {
        if (!this.selectedItem[i]) {
          return false;
        }
      }
      return true;
    },
    onUpdate(index: number, newValue: string) {
      let currentSubTree: Value = this.versionTree
      for (let i = 0; i < index; i++) {
        currentSubTree = currentSubTree.children.get(this.selectedItem[i])!
      }
      for (let i = index + 1; i < this.topLevelGroups.length; i++) {
        this.selectedItem[i] = undefined
        const pos = this.possibleItems[i]
        pos.length = 0
        if (i == index + 1) {
          for (let key of currentSubTree.children.get(newValue)!.children.keys()) {
            pos.push(key)
          }
        }
        this.bestLatestVersions[i] = calculateBestLatest(pos)
        pos.update()
      }
      this.selectedItem.update()

      if (index == this.topLevelGroups.length - 1) {
        this.$emit('update:modelValue', currentSubTree.children.get(newValue)!.children.get('full')!.name)
      } else if (this.modelValue) {
        this.$emit('update:modelValue', null);
      }
    },

    versionDisplay(version: string, group: string, index: number): string {
      let customDisplay = this.displayPattern[group]
      if (customDisplay) {
        customDisplay = customDisplay.replace('${version}', version)
        for (let i = 0; i < index; i++) {
          customDisplay = customDisplay.replace('${version.' + this.topLevelGroups[index] + '}', this.selectedItem[i])
        }
        return customDisplay
      }
      return version
    }
  },
  props: ['versions', 'pattern', 'displayPattern', 'modelValue', 'errors'],
  emits: ['update:modelValue']
}
</script>
