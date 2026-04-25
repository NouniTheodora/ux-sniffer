<template>
  <div>{{ data }}</div>
</template>

<!-- SMELL: Any Type — uses `: any` disabling type checking.
     Expected warning: "TypeScript 'any' type used..." -->

<script setup lang="ts">
import { ref } from 'vue'

const data = ref<any>(null)

function process(input: any): any {
  return input
}
</script>
