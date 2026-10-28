(function () {
  var KEY = "gridwatch-theme";
  var stored = null;
  try { stored = localStorage.getItem(KEY); } catch (e) {}
  var dark = window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches;
  document.documentElement.dataset.theme = stored || (dark ? "dark" : "light");

  document.addEventListener("DOMContentLoaded", function () {
    var btn = document.getElementById("themeToggle");
    if (!btn) return;
    btn.addEventListener("click", function () {
      var next = document.documentElement.dataset.theme === "dark" ? "light" : "dark";
      document.documentElement.dataset.theme = next;
      try { localStorage.setItem(KEY, next); } catch (e) {}
      document.dispatchEvent(new CustomEvent("themechange", { detail: next }));
    });
  });
})();