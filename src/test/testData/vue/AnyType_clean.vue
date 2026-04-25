<template>
  <div>{{ label }}</div>
</template>

<!-- NO SMELL: All types are explicitly defined. -->

<script setup lang="ts">
import { ref } from 'vue'

const label = ref<string>('Hello')

function greet(name: string): string {
  return `Hello, ${name}`
}
</script>
