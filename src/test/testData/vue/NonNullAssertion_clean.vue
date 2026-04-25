<template>
  <div>{{ item?.value }}</div>
</template>

<!-- NO SMELL: Uses optional chaining instead of non-null assertions. -->

<script setup lang="ts">
import { ref } from 'vue'

interface Item { key: number; value: string }

const item = ref<Item | null>(null)

function getValue(): string {
  return item.value?.value ?? 'default'
}
</script>
