<template>
  <div>{{ foundValue }}</div>
</template>

<!-- SMELL: Non-Null Assertion — uses '!' operator to bypass null checks.
     Expected warning: "Non-null assertion operator ('!') used..." -->

<script setup lang="ts">
import { ref } from 'vue'

interface Item { key: number; value: string }

const items = ref<Item[]>([])

const found = items.value.find(item => item.key === 1)
const foundValue = found!.value
</script>
