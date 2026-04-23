<template>
  <div>{{ title }} — {{ count }}</div>
</template>

<script setup>
defineProps({
  title: String,
  count: Number,
  active: Boolean,
  label: String,
  size: String
})
</script>
