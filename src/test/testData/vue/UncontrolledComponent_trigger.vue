<template>
  <form>
    <input ref="nameInput" type="text" />
  </form>
</template>

<!-- SMELL: Uncontrolled Component — input uses ref but no v-model or :value.
     Expected warning: "Uncontrolled <input>: uses a ref but has no v-model or :value binding. Bind the value reactively instead." -->

<script setup>
import { ref } from 'vue'

const nameInput = ref(null)
</script>
