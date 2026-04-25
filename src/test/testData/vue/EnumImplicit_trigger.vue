<template>
  <div>{{ selectedColor }}</div>
</template>

<!-- SMELL: Enum Implicit Values — members have no explicit values.
     Expected warning: "Enum 'Color' has implicit values..." -->

<script setup lang="ts">
import { ref } from 'vue'

enum Color {
  Red,
  Green,
  Blue,
}

const selectedColor = ref(Color.Red)
</script>
