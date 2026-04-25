<template>
  <div>{{ name }} - {{ count }}</div>
</template>

<!-- SMELL: Props in Initial State — multiple props used to initialise refs.
     Expected warning mentions both 'initialName' and 'initialCount'. -->

<script setup>
import { ref } from 'vue'

const props = defineProps({ initialName: String, initialCount: Number })

const name = ref(props.initialName)
const count = ref(props.initialCount)
</script>
