<template>
  <div ref="titleRef">{{ title }}</div>
</template>

<!-- NO SMELL: Uses Vue template refs — no direct DOM manipulation. -->

<script setup>
import { ref, onMounted } from 'vue'

const titleRef = ref(null)
const title = ref('Hello')

onMounted(() => {
  console.log(titleRef.value)
})
</script>
