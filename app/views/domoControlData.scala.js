@(systems: play.api.libs.json.JsObject, scenes: play.api.libs.json.JsArray, monitors: play.api.libs.json.JsArray)
@import play.api.libs.json.Json
'use strict';

var DomoData = (function() {

  var systems =  @JavaScript(Json.stringify(systems));

  var scenes = @JavaScript(Json.stringify(scenes));

  var monitors = @JavaScript(Json.stringify(monitors));

  function getSwitches() {
    var switches = [];
    for(var i in systems) {
      switches = switches.concat(systems[i].switches);
    }

    return switches;
  }

  function getSwitchesBySystem(systemId) {
    return systems[systemId].switches;
  }

  return {
    systems: systems,
    scenes: scenes,
    monitors: monitors,
    switches: getSwitches,
    switchesBySystem: getSwitchesBySystem
  };
}());
