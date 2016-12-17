'use strict';

var Systems = (function() {

  var systems = {
      1: {
        name: 'Philips Hue',
        switches: []
      },
      2: {
        name: 'Wemo',
        switches: []
      }
  }

  function init() {
    loadSwitches();
  }

  function loadSwitches() {
    HttpClient.sendRequest('systems/switches', 'GET', null, function(status, statusText, response) {
      if (status == 200) {
        response.forEach(function (system) {
          systems[system.id].switches = system.switches;
        });

        Switches.showSwitches();
      } else console.log("http error: " + status + " - " + statusText + " - " + response);
    }, {disableAuth: true, loading: false})
  }

  function getSwitches() {
    var switches = [];
    for(var i in systems) {
      switches = switches.concat(systems[i].switches);
    }

    return switches;
  }

  return {
    init: init,
    switches: getSwitches,
    loadSwitches: loadSwitches
  };
}());