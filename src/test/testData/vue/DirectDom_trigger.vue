<template>
  <div id="title">Hello</div>
</template>

<!-- SMELL: Direct DOM Manipulation — uses document.getElementById.
     Expected warning: "Direct DOM manipulation via 'getElementById'. Use Vue template refs instead." -->

<script setup>
function updateTitle() {
  document.getElementById('title').innerText = 'New Title'
}
</script>
