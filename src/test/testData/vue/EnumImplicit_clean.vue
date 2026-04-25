<template>
  <div>{{ selectedColor }}</div>
</template>

<!-- NO SMELL: Enum has explicit string values. -->

<script setup lang="ts">
import { ref } from 'vue'

enum Color {
  Red = 'Red',
  Green = 'Green',
  Blue = 'Blue',
}

const selectedColor = ref<Color>(Color.Red)
</script>
