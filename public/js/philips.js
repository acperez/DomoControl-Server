'use strict';

var Philips = (function() {

  var contentPanelDiv, scenesDiv;

  var serviceId = 1;

  function init() {
    contentPanelDiv = document.getElementById('content_panel');
    scenesDiv = document.getElementById('scenes');

    showScenes();

    window.addEventListener('resize', function(evt) { setContainerWidth(); });

    document.getElementById('scene_btn_on').addEventListener('click', function(evt) { setLightsStatus(true); });
    document.getElementById('scene_btn_off').addEventListener('click', function(evt) { setLightsStatus(false); });
  }

  function showScenes() {
    var scenes = DomoData.scenes;
    scenes.forEach(function(scene) {
      var element = renderScene(scene);
      scenesDiv.appendChild(element);
    });

    var clear = document.createElement('div');
    clear.classList.add('clear');
    scenesDiv.appendChild(clear);

    setContainerWidth();
  }

  function renderScene(scene) {
    var sceneDiv = document.createElement('div');
    sceneDiv.classList.add('scene');

    if (scene.colors.length == 1) sceneDiv.style.background = scene.colors[0];
    else sceneDiv.style.background = 'linear-gradient(to bottom right, ' + scene.colors.toString() + ')';

    var span = document.createElement('span');
    var text = document.createTextNode(scene.name);
    span.classList.add('scene_label');
    span.appendChild(text);
    sceneDiv.appendChild(span)

    sceneDiv.onclick = (function() {
      var id = parseInt(scene.id);
      return function(evt) {
        setLightsColor(id);
      }
    })();

    return sceneDiv;
  }

  function setContainerWidth() {
    //var itemWidth = scenesDiv.childNodes[1].offsetWidth + 20;
    var itemWidth = 124;
    var containerWidth = contentPanelDiv.offsetWidth - 100;
    scenesDiv.style.width = parseInt(containerWidth / itemWidth) * itemWidth + 'px';
  }

  function setLightsStatus(status) {
    var url = 'system/' + serviceId + '/switches/' + (status ? 1 : 0);
    HttpClient.sendRequest(url, 'GET', null, function(status, statusText, response) {
      if (status != 200) console.log("Switch set status error: " + status + " - " + statusText + " - " + response);
    }, {disableAuth: true, loading: false});
  }

  function setLightsColor(sceneId) {
    var url = 'system/' + serviceId + '/switches/extra/';

    var data = {
      "sceneId": sceneId
    };

    HttpClient.sendRequest(url, 'POST', data, function(status, statusText, response) {
      if (status != 200) console.log("Switch set status error: " + status + " - " + statusText + " - " + response);
    }, {disableAuth: true, loading: false});
  }

  return {
    init: init
  };
}());
