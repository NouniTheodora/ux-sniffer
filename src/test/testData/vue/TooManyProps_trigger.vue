<template>
  <div>Too many props example</div>
</template>

<!-- SMELL: Too Many Props — 14 props defined (threshold: 13).
     Expected warning: "Too many props: 14 defined (threshold: 13). Consider grouping related props into objects or splitting the component." -->

<script setup>
defineProps({
  propA: String,
  propB: Number,
  propC: Boolean,
  propD: String,
  propE: String,
  propF: Number,
  propG: Boolean,
  propH: String,
  propI: Array,
  propJ: Object,
  propK: String,
  propL: Number,
  propM: Boolean,
  propN: String
})
</script>
