<template>
  <div>{{ count }}</div>
  <button @click="increment">+1</button>
</template>

<!-- NO SMELL: Uses Vue reactivity — no $forceUpdate or location.reload. -->

<script setup>
import { ref, computed } from 'vue'

const count = ref(0)
const doubled = computed(() => count.value * 2)

function increment() {
  count.value++
}
</script>
