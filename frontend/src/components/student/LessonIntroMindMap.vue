<template>
  <div class="intro-mind-map">
    <div class="mm-wrap">
      <div class="mm-root-col">
        <div class="mm-node mm-root">{{ map.title }}</div>
        <svg class="mm-line mm-line-to-spine" viewBox="0 0 40 2" preserveAspectRatio="none" aria-hidden="true">
          <path d="M0,1 L40,1" fill="none" stroke="currentColor" stroke-width="1.5" />
        </svg>
      </div>

      <div class="mm-tree">
        <div
          v-for="(branch, bi) in map.branches"
          :key="bi"
          class="mm-branch"
        >
          <div class="mm-branch-row">
            <svg class="mm-line mm-line-spine" viewBox="0 0 32 24" preserveAspectRatio="none" aria-hidden="true">
              <path d="M0,12 C10,12 14,12 32,12" fill="none" stroke="currentColor" stroke-width="1.5" />
            </svg>
            <div class="mm-node mm-topic">{{ branch.label }}</div>

            <div class="mm-leaf-stack">
              <div
                v-for="(node, ni) in branch.nodes"
                :key="ni"
                class="mm-leaf-row"
              >
                <svg class="mm-line mm-line-leaf" viewBox="0 0 24 2" preserveAspectRatio="none" aria-hidden="true">
                  <path d="M0,1 L24,1" fill="none" stroke="currentColor" stroke-width="1.5" />
                </svg>
                <div class="mm-node mm-leaf">{{ node.text }}</div>
                <template v-if="node.children?.length">
                  <div
                    v-for="(child, ci) in node.children"
                    :key="ci"
                    class="mm-child-row"
                  >
                    <svg class="mm-line mm-line-child" viewBox="0 0 20 2" preserveAspectRatio="none" aria-hidden="true">
                      <path d="M0,1 L20,1" fill="none" stroke="currentColor" stroke-width="1.5" />
                    </svg>
                    <div class="mm-node mm-sub">{{ child.text }}</div>
                  </div>
                </template>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  map: {
    type: Object,
    required: true,
    default: () => ({ title: '', branches: [] })
  }
})
</script>

<style scoped>
.intro-mind-map {
  min-height: 380px;
  padding: 36px 32px 44px;
  background: #fafbfc;
  border-radius: 12px;
}
.mm-wrap {
  display: flex;
  align-items: center;
  min-height: 300px;
}
.mm-root-col {
  flex-shrink: 0;
  display: flex;
  align-items: center;
}
.mm-node {
  padding: 10px 14px;
  font-size: 14px;
  line-height: 1.45;
  color: #1e293b;
  background: #fff;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.05);
}
.mm-root {
  min-width: 100px;
  max-width: 128px;
  text-align: center;
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
  background: linear-gradient(180deg, #f0f9ff 0%, #e0f2fe 100%);
  border-color: #bae6fd;
  padding: 14px 12px;
}
.mm-line {
  flex-shrink: 0;
  color: #94a3b8;
}
.mm-line-to-spine {
  width: 40px;
  height: 2px;
}
.mm-tree {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 32px;
  min-width: 0;
  border-left: 1.5px solid #cbd5e1;
  padding: 8px 0;
}
.mm-branch-row {
  display: flex;
  align-items: flex-start;
  min-height: 44px;
}
.mm-line-spine {
  width: 32px;
  height: 24px;
  margin-left: -32px;
  margin-top: 14px;
}
.mm-topic {
  flex-shrink: 0;
  font-weight: 700;
  color: #1e3a8a;
  border-color: #bfdbfe;
  white-space: nowrap;
  margin-top: 4px;
}
.mm-leaf-stack {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
  padding-top: 4px;
}
.mm-leaf-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0;
  row-gap: 8px;
}
.mm-line-leaf {
  width: 24px;
  height: 2px;
  margin-left: 12px;
}
.mm-leaf {
  color: #334155;
  max-width: 100%;
}
.mm-child-row {
  display: flex;
  align-items: center;
  margin-left: 4px;
}
.mm-line-child {
  width: 20px;
  height: 2px;
}
.mm-sub {
  color: #475569;
  font-size: 13px;
  background: #f8fafc;
  border-color: #e2e8f0;
}
@media (max-width: 900px) {
  .mm-wrap {
    flex-direction: column;
    align-items: stretch;
  }
  .mm-root-col {
    justify-content: center;
    margin-bottom: 12px;
  }
  .mm-line-to-spine,
  .mm-tree {
    border-left: none;
  }
  .mm-line-spine,
  .mm-line-leaf,
  .mm-line-child {
    display: none;
  }
  .mm-branch-row {
    flex-direction: column;
    gap: 10px;
    padding-left: 8px;
  }
  .mm-leaf-row {
    flex-direction: column;
    align-items: flex-start;
    padding-left: 12px;
  }
  .mm-child-row {
    margin-left: 12px;
  }
}
</style>
