<template>
  <router-view />
</template>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
:root {
  --ls-primary: #4f46e5;
  --ls-primary-deep: #3730a3;
  --ls-accent: #6366f1;
  --ls-surface: #eef1f8;
  --ls-card-bg: #ffffff;
  --ls-card-shadow: 0 8px 32px rgba(15, 23, 42, 0.08);
  --ls-card-shadow-hover: 0 14px 40px rgba(79, 70, 229, 0.14);
  --ls-radius-lg: 16px;
  --ls-radius-md: 12px;
}
body {
  font-family: 'PingFang SC', 'Microsoft YaHei', system-ui, sans-serif;
  background: var(--ls-surface);
  color: #1e293b;
  -webkit-font-smoothing: antialiased;
}
</style>
