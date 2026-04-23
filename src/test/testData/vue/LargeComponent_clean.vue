<template>
  <div>
    <p>{{ message }}</p>
    <button @click="handleClick">Click me</button>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const message = ref('Hello')

function handleClick() {
  message.value = 'Clicked!'
}

const reset = () => {
  message.value = 'Hello'
}
</script>
