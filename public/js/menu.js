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
    addMenuItem('menu_wemo', 'wemo');
    addMenuItem('menu_events', 'fake');

    settings.panel = document.getElementById('switches_settings');
    settings.selection = document.getElementById('menu_settings').children[1];
    addMenuItem('menu_settings_switches', 'switches_settings');
    addMenuItem('menu_settings_philips_hue', 'philips_hue_settings');
    addMenuItem('menu_settings_wemo', 'wemo_settings');

    document.getElementById('settingsBtn').addEventListener('click', function() { Animation.menuAnimationStart(); });
  }

  function addMenuItem(menuId, panelId) {
    var panelElement = document.getElementById(panelId);
    var menuElement = document.getElementById(menuId);
    menuElement.addEventListener('click', function() { menuItemSelected(menuElement, panelElement); });
  }

  function switchMenuPanel() {
    if (settingsActive) {
      menu_items.style.display = 'block';
      menu_settings.style.display = 'none';
      settings.selection.removeChild(selector);
      application.selection.appendChild(selector);

      settings.panel.classList.remove('content_on');
      application.panel.classList.add('content_on');
    } else {
      menu_items.style.display = 'none';
      menu_settings.style.display = 'block';
      application.selection.removeChild(selector);
      settings.selection.appendChild(selector);

      application.panel.classList.remove('content_on');
      settings.panel.classList.add('content_on');
    }

    settingsActive = !settingsActive;
  }

  function menuItemSelected(item, panel) {
    if (settingsActive) {
      selectItem(settings, item, panel);
    } else {
      selectItem(application, item, panel);
    }
  }

  function selectItem(menuType, item, panel) {
    menuType.selection.removeChild(selector);
    menuType.selection.classList.remove('menu_item_on');
    menuType.panel.classList.remove('content_on');

    item.appendChild(selector);
    item.classList.add('menu_item_on');
    panel.classList.add('content_on');

    menuType.selection = item;
    menuType.panel = panel;
  }

  return {
    init: init,
    switchMenuPanel: switchMenuPanel
  };
}());
