'use strict';

var Switches = (function() {

  var containerDiv, emptyMsgDiv, listDiv;

  function init() {
    containerDiv = document.getElementById('switches_container');
    emptyMsgDiv = document.getElementById('switches_empty');
    listDiv = document.getElementById('switches_items');

    window.addEventListener('resize', function(evt) { setContainerWidth(); });

    showSwitches()
  }

  function showSwitches() {
    var switches = DomoData.switches();
    if (switches.length == 0) {
      emptyMsgDiv.style.display = 'flex';
      listDiv.style.display = 'none';
      return;
    }

    emptyMsgDiv.style.display = 'none';
    listDiv.style.display = 'block';

    // Show items
    switches.forEach(function(domoSwitch) {
      if (domoSwitch.available) {
        var element = renderSwitch(domoSwitch);
        listDiv.appendChild(element);
      }
    });

    var clear = document.createElement('div');
    clear.classList.add('clear');
    listDiv.appendChild(clear);

    setContainerWidth();
  }

  function setContainerWidth() {
    var itemWidth = listDiv.childNodes[1].offsetWidth;
    var containerWidth = containerDiv.offsetWidth;
    listDiv.style.width = parseInt(containerWidth / itemWidth) * itemWidth + 'px';
  }

  function renderSwitch(domoSwitch) {
    var main = document.createElement('div');
    main.classList.add('button2');

    var label = document.createElement('div');
    label.classList.add('btn_label');
    var text = document.createTextNode(domoSwitch.name);
    label.appendChild(text);

    var icon = document.createElement('a');
    var img = document.createTextNode('\uF011');
    icon.appendChild(img);
    addSwitchEvent(domoSwitch, icon);

    if (domoSwitch.status) icon.classList.add('on');

    main.appendChild(label);
    main.appendChild(icon);

    return main;
  }

  function addSwitchEvent(domoSwitch, button) {
    button.addEventListener('click', function(evt) {
      var status = button.classList.contains('on');
      status ? button.classList.remove('on') : button.classList.add('on');

      var url = 'system/' + domoSwitch.serviceId + '/switch/' + domoSwitch.id + '/' + (status ? 0 : 1);
      HttpClient.sendRequest(url, 'GET', null, function(status, statusText, response) {
        if (status != 200) console.log("Switch set status error: " + status + " - " + statusText + " - " + response);
      }, {disableAuth: true, loading: false})
    });
  }

  return {
    init: init
  };
}());
