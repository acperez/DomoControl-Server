'use strict';

function init() {
  Menu.init();
  Switches.init();
  Philips.init();
  Wemo.init();
  SwitchesSettings.init();
}

function loadSwitches() {
  HttpClient.sendRequest('systems/switches', 'GET', null, function(status, statusText, response) {
    if (status == 200) {
      response.forEach(function (system) {
        systems[system.id].switches = system.switches;
      });


    } else console.log("http error: " + status + " - " + statusText + " - " + response);
  }, {disableAuth: true, loading: false})
}
