@(systems: play.api.libs.json.JsObject, scenes: play.api.libs.json.JsArray)
@import play.api.libs.json.Json
'use strict';

var DomoData = (function() {

  var systems =  @JavaScript(Json.stringify(systems));

  var scenes = @JavaScript(Json.stringify(scenes));

  function getSwitches() {
    var switches = [];
    for(var i in systems) {
      switches = switches.concat(systems[i].switches);
    }

    return switches;
  }

  return {
    systems: systems,
    scenes: scenes,
    switches: getSwitches
  };
}());
