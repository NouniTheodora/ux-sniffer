<template>
  <div class="greeting">
    <h1>{{ title }}</h1>
    <p>{{ message }}</p>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const title = ref('Hello')
const message = ref('This is a clean, small component.')
</script>

<style scoped>
.greeting {
  font-family: sans-serif;
}
</style>
