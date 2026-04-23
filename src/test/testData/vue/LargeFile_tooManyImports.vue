<template>
  <div>Too many imports example</div>
  <div>Too many imports example</div>
  <div>Too many imports example</div>
</template>

<!-- SMELL: Large File — this file has 21 imports (IMPORTS_THRESHOLD = 20).
     Expected warning: "Too many imports: 21 (threshold: 20). This file may be taking on too many responsibilities." -->

<script setup>
import CompA from './CompA.vue'
import CompB from './CompB.vue'
import CompC from './CompC.vue'
import CompD from './CompD.vue'
import CompE from './CompE.vue'
import CompF from './CompF.vue'
import CompG from './CompG.vue'
import CompH from './CompH.vue'
import CompI from './CompI.vue'
import CompJ from './CompJ.vue'
import CompK from './CompK.vue'
import CompL from './CompL.vue'
import CompM from './CompM.vue'
import CompN from './CompN.vue'
import CompO from './CompO.vue'
import CompP from './CompP.vue'
import CompQ from './CompQ.vue'
import CompR from './CompR.vue'
import CompS from './CompS.vue'
import CompT from './CompT.vue'
import CompU from './CompU.vue'
</script>
