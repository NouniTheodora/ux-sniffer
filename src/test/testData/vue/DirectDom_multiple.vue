<template>
  <div id="app">
    <ul id="list"></ul>
  </div>
</template>

<!-- SMELL: Direct DOM Manipulation — multiple DOM APIs used.
     Expected warning mentions getElementById, createElement, and appendChild. -->

<script setup>
function addItem(text) {
  const list = document.getElementById('list')
  const item = document.createElement('li')
  item.textContent = text
  list.appendChild(item)
}
</script>
