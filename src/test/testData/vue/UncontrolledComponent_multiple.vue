<template>
  <form>
    <input ref="nameInput" type="text" />
    <textarea ref="messageInput"></textarea>
  </form>
</template>

<!-- SMELL: Uncontrolled Component — two form elements use ref without v-model or :value.
     Expected warning: "2 uncontrolled form elements use a ref but have no v-model or :value binding." -->

<script setup>
import { ref } from 'vue'

const nameInput = ref(null)
const messageInput = ref(null)
</script>
