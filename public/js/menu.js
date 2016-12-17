'use strict';

var Menu = (function() {

  var selector = null;

  var settingsActive = false;

  var application = {
    panel: null,
    selection: null
  };

  var settings = {
    panel: null,
    selection: null
  };

  function init() {
    Animation.init();

    selector = document.getElementById('menu_selector');
    application.panel = document.getElementById('menu_items');
    application.selection = application.panel.children[1];
    addMenuItem('menu_switches');
    addMenuItem('menu_philips_hue');
    addMenuItem('menu_wemo');
    addMenuItem('menu_events');

    settings.panel = document.getElementById('menu_settings');
    settings.selection = settings.panel.children[1];
    addMenuItem('menu_settings_philips_hue');
    addMenuItem('menu_settings_wemo');

    document.getElementById('settingsBtn').addEventListener('click', function(evt) { Animation.menuAnimationStart(); });
  }

  function addMenuItem(id) {
    var element = document.getElementById(id);
    element.addEventListener('click', function(evt) { menuItemSelected(element); });
  }

  function switchMenuPanel() {
    if (settingsActive) {
      menu_items.style.display = 'block';
      menu_settings.style.display = 'none';
      settings.selection.removeChild(selector);
      application.selection.appendChild(selector);
    } else {
      menu_items.style.display = 'none';
      menu_settings.style.display = 'block';
      application.selection.removeChild(selector);
      settings.selection.appendChild(selector);
    }

    settingsActive = !settingsActive;
  }

  function menuItemSelected(item) {
    if (settingsActive) {
      settings.selection.removeChild(menu.selector);
      item.appendChild(selector);
      settings.selection = item;
    } else {
      application.selection.removeChild(menu.selector);
      item.appendChild(selector);
      application.selection = item;
    }
  }

  return {
    init: init,
    switchMenuPanel: switchMenuPanel
  };
}());
