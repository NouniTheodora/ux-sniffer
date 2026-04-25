<template>
  <div>Force update example</div>
</template>

<!-- SMELL: Force Update — uses $forceUpdate().
     Expected warning: "Avoid '$forceUpdate()'. Redesign state so Vue's reactivity handles updates automatically." -->

<script setup>
import { getCurrentInstance } from 'vue'

const instance = getCurrentInstance()

function refresh() {
  instance.proxy.$forceUpdate()
}
</script>
