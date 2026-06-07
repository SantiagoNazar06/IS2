// dark-mode.js — Gestor de modo oscuro global
(function() {
  'use strict';
  
  const STORAGE_KEY = 'theme';
  const DARK_CLASS = 'dark';

  // Helper para localStorage con fallback
  function getSavedTheme() {
    try { return localStorage.getItem(STORAGE_KEY); } catch(e) { return null; }
  }
  function setSavedTheme(val) {
    try { localStorage.setItem(STORAGE_KEY, val); } catch(e) { /* noop */ }
  }
  
  // 1. Get saved preference or system preference
  let theme = getSavedTheme();
  if (!theme) {
    theme = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }
  
  // 2. Apply immediately (FOLM prevention)
  if (theme === 'dark') {
    document.documentElement.classList.add(DARK_CLASS);
  }
  
  // 3. When DOM is ready, create the toggle button
  function init() {
    const btn = document.getElementById('darkModeToggle');
    if (!btn) return;
    
    function updateIcon() {
      const isDark = document.documentElement.classList.contains(DARK_CLASS);
      btn.textContent = isDark ? '\u2600\uFE0F' : '\uD83C\uDF19';
      btn.style.background = isDark ? '#f9fafb' : '#1f2937';
      btn.style.color = isDark ? '#1f2937' : '#f9fafb';
      btn.style.borderColor = isDark ? '#d1d5db' : '#374151';
    }
    
    btn.addEventListener('click', function() {
      document.documentElement.classList.toggle(DARK_CLASS);
      const isDark = document.documentElement.classList.contains(DARK_CLASS);
      setSavedTheme(isDark ? 'dark' : 'light');
      updateIcon();
    });
    
    updateIcon();
  }
  
  // Run on DOMContentLoaded or immediately if already loaded
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
  
  // Listen for system preference changes
  window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', function(e) {
    if (!getSavedTheme()) {
      if (e.matches) {
        document.documentElement.classList.add(DARK_CLASS);
      } else {
        document.documentElement.classList.remove(DARK_CLASS);
      }
    }
  });
})();
