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
    application.panel = document.getElementById('switches');
    application.selection = document.getElementById('menu_items').children[1];

    addMenuItem('menu_switches', 'switches');
    addMenuItem('menu_philips_hue', 'philips_hue');
    addMenuItem('menu_wemo', 'fake');
    addMenuItem('menu_events', 'fake');

    settings.panel = document.getElementById('menu_settings');
    settings.selection = settings.panel.children[1];
    addMenuItem('menu_settings_philips_hue', 'fake');
    addMenuItem('menu_settings_wemo', 'fake');

    document.getElementById('settingsBtn').addEventListener('click', function(evt) { Animation.menuAnimationStart(); });
  }

  function addMenuItem(menuId, panelId) {
    var panelElement = document.getElementById(panelId);
    var menuElement = document.getElementById(menuId);
    menuElement.addEventListener('click', function(evt) { menuItemSelected(menuElement, panelElement); });
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

  function menuItemSelected(item, panel) {
    if (settingsActive) {
      settings.selection.removeChild(selector);
      settings.selection.classList.remove('menu_item_on');
      item.appendChild(selector);
      item.classList.add('menu_item_on');
      settings.selection = item;
    } else {
      application.selection.removeChild(selector);
      application.selection.classList.remove('menu_item_on');
      application.panel.classList.remove('content_on');

      item.appendChild(selector);
      item.classList.add('menu_item_on');
      panel.classList.add('content_on');

      application.selection = item;
      application.panel = panel;
    }
  }

  return {
    init: init,
    switchMenuPanel: switchMenuPanel
  };
}());
