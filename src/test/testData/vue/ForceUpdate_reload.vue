<template>
  <div>Reload example</div>
</template>

<!-- SMELL: Force Update — uses location.reload().
     Expected warning: "Avoid 'location.reload()'. Use Vue's reactivity or router navigation instead of full page reloads." -->

<script setup>
function hardRefresh() {
  location.reload()
}
</script>
