'use strict';

var Utils = (function() {

  var popup = null;

  function renderPopup(title) {
    if (!popup) {
      var background = document.createElement('div');
      background.id = 'popup';
      background.classList.add('popup_background');

      popup = document.createElement('div');
      background.appendChild(popup);

      var titleDiv = document.createElement('div');
      titleDiv.classList.add('popup_title');
      titleDiv.appendChild(document.createTextNode(title));

      var contentDiv = document.createElement('div');
      contentDiv.classList.add('popup_content');

      popup.appendChild(titleDiv);
      popup.appendChild(contentDiv);

      var body = document.body;
      body.appendChild(background);

      document.documentElement.style.overflow = 'hidden';
      return contentDiv;
    }

    var container = popup.parentElement;
    container.removeChild(popup);
    popup = document.createElement('div');
    container.appendChild(popup);
    return popup;
  }

  function hidePopup() {
    if (popup) {
      document.body.removeChild(document.getElementById('popup'));
      document.documentElement.style.overflow = '';
      popup = null;
    }
  }

  function inputTextPopup(title, msg, placeHolder, ok, cancel, callback) {
    var popup = renderPopup(title);

    var msgTxt = document.createElement('p');
    msgTxt.classList.add('popup_msg');
    msgTxt.appendChild(document.createTextNode(msg));

    var user = document.createElement('input');
    user.type = 'text';
    user.classList.add('popup_input');
    user.placeholder = placeHolder;
    user.maxLength = 256;
    user.oninput = (function(event) {
      if(event.target.value.length > 0) okBtn.classList.remove('disabled');
      else okBtn.classList.add('disabled');
    });

    var cancelBtn = document.createElement('a');
    cancelBtn.classList.add('popup_btn', 'btn', 'left');
    cancelBtn.appendChild(document.createTextNode(cancel));
    cancelBtn.onclick = (function() {
      callback();
    });

    var okBtn = document.createElement('a');
    okBtn.classList.add('btn', 'popup_btn', 'right', 'disabled');
    okBtn.appendChild(document.createTextNode(ok));
    okBtn.onclick = (function() {
      callback(user.value);
    });

    var clear = document.createElement('p');
    clear.classList.add('clear');

    var btnPanel = document.createElement('div');
    btnPanel.appendChild(okBtn);
    btnPanel.appendChild(cancelBtn);
    btnPanel.appendChild(clear);

    popup.appendChild(msgTxt);
    popup.appendChild(user);
    popup.appendChild(btnPanel);

    user.focus();
  }

  function infoPopup(title, msg, ok, callback) {
    var popup = renderPopup(title);

    var msgTxt = document.createElement('p');
    msgTxt.classList.add('popup_msg');
    msgTxt.appendChild(document.createTextNode(msg));

    var okBtn = document.createElement('a');
    okBtn.classList.add('btn', 'popup_btn');
    okBtn.appendChild(document.createTextNode(ok));
    okBtn.onclick = (function() {
      callback();
    });

    var btnPanel = document.createElement('div');
    btnPanel.classList.add('center');
    btnPanel.appendChild(okBtn);

    popup.appendChild(msgTxt);
    popup.appendChild(btnPanel);
  }

  return {
    showInputTextPopup: inputTextPopup,
    showInfoPopup: infoPopup,
    removePopup: hidePopup
  };
}());
