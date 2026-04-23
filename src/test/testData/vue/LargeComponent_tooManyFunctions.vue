<template>
  <div>Too many functions example</div>
</template>

<!-- SMELL: Large Component — 5 functions defined (threshold: 4).
     Expected warning: "Large component: 5 functions defined (threshold: 4). Consider extracting logic into composables." -->

<script setup>
import { ref } from 'vue'

const count = ref(0)

function handleClick() {
  count.value++
}

function handleHover() {
  console.log('hovered')
}

function handleFocus() {
  console.log('focused')
}

const handleBlur = () => {
  console.log('blurred')
}

const handleSubmit = async () => {
  await api.submit(count.value)
}
</script>
