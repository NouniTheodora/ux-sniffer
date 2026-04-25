<template>
  <div>Inherited component</div>
</template>

<!-- SMELL: Inheritance Instead of Composition — uses extends: BaseComponent.
     Expected warning: "Inheritance via 'extends: BaseComponent' detected. Prefer composables or component composition over class-style inheritance." -->

<script>
import BaseComponent from './BaseComponent.vue'

export default {
  extends: BaseComponent,
  data() {
    return { extra: true }
  }
}
</script>
