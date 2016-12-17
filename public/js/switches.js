'use strict';

var Switches = (function() {

  var emptyMsgDiv, listDiv;

  function init() {
    emptyMsgDiv = document.getElementById('switches_empty');
    listDiv = document.getElementById('switches_items');
  }

  function showSwitches() {
    var switches = Systems.switches();
    if (switches.length == 0) {
      emptyMsgDiv.style.display = 'block';
      listDiv.style.display = 'none';
      return;
    }

    emptyMsgDiv.style.display = 'none';
    listDiv.style.display = 'block';

    // Show items
    switches.forEach(function(domoSwitch) {
      console.log(JSON.stringify(domoSwitch));
    })
  }

  return {
    init: init,
    showSwitches: showSwitches
  };
}());
