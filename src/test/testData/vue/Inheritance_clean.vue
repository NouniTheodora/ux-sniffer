<template>
  <div>{{ formatted }}</div>
</template>

<!-- NO SMELL: Uses composables instead of inheritance. -->

<script setup>
import { useFormatter } from './composables/useFormatter'

const { formatted } = useFormatter('hello')
</script>
