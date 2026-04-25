<template>
  <div>{{ displayName }}</div>
</template>

<!-- NO SMELL: Uses computed() to derive values from props — stays in sync. -->

<script setup>
import { computed } from 'vue'

const props = defineProps({ initialName: String, count: Number })

const displayName = computed(() => props.initialName)
const doubled = computed(() => props.count * 2)
</script>
