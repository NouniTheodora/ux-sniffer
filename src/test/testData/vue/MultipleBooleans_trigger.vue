<template>
  <div>Countdown</div>
</template>

<!-- SMELL: Multiple Booleans for State — 5 boolean refs (threshold: 4).
     Expected warning: "Multiple booleans for state: 5 boolean refs defined..." -->

<script setup lang="ts">
import { ref } from 'vue'

const isLoading = ref(true)
const isRunning = ref(false)
const isPaused = ref(false)
const isFinished = ref(false)
const hasError = ref(false)

function start() {
  isRunning.value = true
  isFinished.value = true
}
</script>
