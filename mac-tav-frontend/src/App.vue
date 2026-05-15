<template>
  <a-layout class="app-shell">
    <a-layout-header class="app-header">
      <RouterLink class="brand" to="/">
        <span class="brand-mark">M</span>
        <span>MAC-TAV 网络意图翻译与闭环验证系统</span>
      </RouterLink>
    </a-layout-header>
    <a-layout-content class="app-content">
      <RouterView />
    </a-layout-content>
  </a-layout>
</template>
