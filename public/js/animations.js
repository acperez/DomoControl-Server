'use strict';

var Animation = (function() {

  var settingsBtn, menu;

  var running = false;

  var transitionEnd = null;

  function init() {
    settingsBtn = document.getElementById('settingsBtn');
    menu = document.getElementById('menu');
    transitionEnd = getPrefix('TransitionEnd');
  }

  function getPrefix(value) {
    var styles = window.getComputedStyle(document.documentElement, '');
    var prefix = (Array.prototype.slice
      .call(styles)
      .join('')
      .match(/-(moz|webkit|ms)-/) || (styles.OLink === '' && ['', 'o'])
    )[1];
    return prefix + value;
  }

  function menuAnimationStart() {
    if (running) return;

    running = true;

    settingsBtn.classList.contains('enabled') ? settingsBtn.classList.remove('enabled') : settingsBtn.classList.add('enabled');

    menu.addEventListener(transitionEnd, anim1, false);
    menu.classList.add('menu_anim');
  }

  function anim1(event) {
    event.stopPropagation();
    event.target.removeEventListener(transitionEnd, anim1);

    Menu.switchMenuPanel();

    menu.addEventListener(transitionEnd, anim2, false);
    menu.classList.remove('menu_anim_fade_out');
    menu.classList.add('menu_anim_fade_in');
    menu.classList.remove('menu_anim');
  }

  function anim2(event) {
    event.stopPropagation();
    event.target.removeEventListener(transitionEnd, anim2);

    running = false;
  }

  return {
    init: init,
    menuAnimationStart: menuAnimationStart
  };
}());
