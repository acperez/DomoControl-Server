'use strict';

var Switches = (function() {

  var containerDiv, emptyMsgDiv, listDiv;

  function init() {
    containerDiv = document.getElementById('switches_container');
    emptyMsgDiv = document.getElementById('switches_empty');
    listDiv = document.getElementById('switches_items');

    window.addEventListener('resize', function(evt) { setContainerWidth(); });
  }

  function showSwitches() {
    var switches = Systems.switches();
    if (switches.length == 0) {
      emptyMsgDiv.style.display = 'flex';
      listDiv.style.display = 'none';
      return;
    }

    emptyMsgDiv.style.display = 'none';
    listDiv.style.display = 'block';

    // Show items
    switches.forEach(function(domoSwitch) {
      console.log(JSON.stringify(domoSwitch));
      var element = renderSwitch(domoSwitch);
      listDiv.appendChild(element);
    });

    var clear = document.createElement('div');
    clear.style.clear = 'both';
    listDiv.appendChild(clear);

    setContainerWidth();
  }

  function setContainerWidth() {
    var itemWidth = listDiv.childNodes[1].offsetWidth;
    var containerWidth = containerDiv.offsetWidth;
    listDiv.style.width = parseInt(containerWidth / itemWidth) * itemWidth + 'px';
    console.log('hia')
  }

  function renderSwitch(domoSwitch) {
    var main = document.createElement('div');
    main.classList.add('button2');

    if (domoSwitch.status) main.classList.add('on');

    var label = document.createElement('div');
    label.classList.add('btn_label');
    var text = document.createTextNode(domoSwitch.name);
    label.appendChild(text);

    var icon = document.createElement('a');
    var img = document.createTextNode('\uF011');
    icon.appendChild(img);
    addSwitchEvent(domoSwitch, icon);

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
    init: init,
    showSwitches: showSwitches
  };
}());
