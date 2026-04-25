<template>
  <div>{{ state }}</div>
</template>

<!-- NO SMELL: Uses a union type for state instead of multiple booleans. -->

<script setup lang="ts">
import { ref } from 'vue'

type CountdownState = 'loading' | 'running' | 'paused' | 'finished' | 'error'

const state = ref<CountdownState>('loading')

function start() {
  state.value = 'running'
}
</script>
