<template>
  <div>Both violations</div>
</template>

<!-- SMELL: Force Update — uses both $forceUpdate() and location.reload().
     Expected warning mentions both APIs. -->

<script setup>
import { getCurrentInstance } from 'vue'

const instance = getCurrentInstance()

function refresh() {
  instance.proxy.$forceUpdate()
}

function hardRefresh() {
  location.reload()
}
</script>
