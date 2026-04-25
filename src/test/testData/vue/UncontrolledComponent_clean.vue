<template>
  <form>
    <input v-model="name" type="text" />
    <input :value="email" @input="onInput" type="email" />
    <textarea v-model="message"></textarea>
    <select v-model="role">
      <option>Admin</option>
      <option>User</option>
    </select>
  </form>
</template>

<!-- NO SMELL: All form elements are controlled via v-model or :value. -->

<script setup>
import { ref } from 'vue'

const name = ref('')
const email = ref('')
const message = ref('')
const role = ref('User')

function onInput(e) {
  email.value = e.target.value
}
</script>
