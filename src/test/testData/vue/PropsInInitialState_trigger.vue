<template>
  <div>{{ name }}</div>
</template>

<!-- SMELL: Props in Initial State — ref(props.initialName) breaks one-way data flow.
     Expected warning: "Prop 'initialName' used to initialise reactive state. Use computed(() => props.initialName) to stay in sync." -->

<script setup>
import { ref } from 'vue'

const props = defineProps({ initialName: String })

const name = ref(props.initialName)
</script>
