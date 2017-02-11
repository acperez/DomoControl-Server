'use strict';

var Philips = (function() {

  var contentPanelDiv, scenesPanel, colorPickerPanel, scenesDiv, lightListDiv, colorPickerDiv;
  var hueSlider, satSlider, briSlider;
  var scenesEditEnabled = false;

  var serviceId = 1;

  var color = {
    hue: 0,
    sat: 100,
    bri: 100
  };

  var lightsGroup = [];

  function init() {
    contentPanelDiv = document.getElementById('content_panel');
    scenesPanel = document.getElementById('scenes_panel');
    colorPickerPanel = document.getElementById('color_picker_panel');
    lightListDiv = document.getElementById('light_list');
    colorPickerDiv = document.getElementById('color_picker');

    document.getElementById('scene_btn_save').addEventListener('click', function(evt) { saveScene(); });
    document.getElementById('picker_btn_on').addEventListener('click', function(evt) { setPickerStatus(true); });
    document.getElementById('picker_btn_off').addEventListener('click', function(evt) { setPickerStatus(false); });

    scenesDiv = document.getElementById('scenes');

    showScenes();

    loadLights();

    window.addEventListener('resize', function(evt) { setContainerWidth(); });

    document.getElementsByName('philips_toggle').forEach(function(toggleButton) {
      toggleButton.addEventListener('click', function(evt) { togglePanel(toggleButton.value); })
    });

    document.getElementById('scene_btn_edit').addEventListener('click', function(evt) { toggleScenesEdit(); });

    hueSlider = document.getElementById('hue_slider');
    hueSlider.addEventListener('input', function(evt) { setPickerHue(hueSlider.valueAsNumber); });
    document.getElementById('hue_dec').addEventListener('click', function(evt) { changePickerHue(-1); });
    document.getElementById('hue_inc').addEventListener('click', function(evt) { changePickerHue(1); });

    satSlider = document.getElementById('sat_slider');
    satSlider.addEventListener('input', function(evt) { setPickerSat(satSlider.valueAsNumber); });
    document.getElementById('sat_dec').addEventListener('click', function(evt) { changePickerSat(-1); });
    document.getElementById('sat_inc').addEventListener('click', function(evt) { changePickerSat(1); });

    briSlider = document.getElementById('bri_slider');
    briSlider.addEventListener('input', function(evt) { setPickerBri(briSlider.valueAsNumber); });
    document.getElementById('bri_dec').addEventListener('click', function(evt) { changePickerBri(-1); });
    document.getElementById('bri_inc').addEventListener('click', function(evt) { changePickerBri(1); });
  }

  function togglePanel(value) {
    if (value == 'scenes') {
      scenesPanel.style.display = 'block';
      colorPickerPanel.style.display = 'none';
    } else if (value == 'lights') {
      scenesPanel.style.display = 'none';
      colorPickerPanel.style.display = 'block';
    }
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
    var text = document.createTextNode(scene.name.charAt(0).toUpperCase() + scene.name.slice(1));
    span.classList.add('scene_label');
    span.appendChild(text);
    sceneDiv.appendChild(span);

    sceneDiv.onclick = (function() {
      var id = scene.name;
      return function(evt) {
        setLightsColor(id);
      }
    })();

    if (!scene.default) {
      var img = document.createElement('img');
      img.classList.add('scene_delete');
      img.src = 'img/close.svg';
      img.onclick = (function () {
        return function(evt) {
          evt.stopPropagation();
          sendRemoveScene(scene.name, function(status) {
            toggleScenesEdit();
          })
        }
      })();

      sceneDiv.appendChild(img);
    }

    return sceneDiv;
  }

  function toggleScenesEdit() {
    if (scenesEditEnabled) {
      scenesDiv.childNodes.forEach(function(sceneDiv) {
        sceneDiv.classList.remove('scene_edit');
        var img = sceneDiv.getElementsByTagName('img')[0];
        if (img) img.style.display = 'none';
      });
      scenesEditEnabled = false;
    } else {
      scenesDiv.childNodes.forEach(function(sceneDiv) {
          sceneDiv.classList.add('scene_edit');
          var img = sceneDiv.getElementsByTagName('img')[0];
        if (img) img.style.display = 'block';
      });
      scenesEditEnabled = true;
    }
  }

  function loadLights() {
    var lights = DomoData.switchesBySystem(1);
    lights.forEach(function (light) {
      var lightDiv = renderLight(light);
      lightListDiv.appendChild(lightDiv)
    })
  }

  function renderLight(light) {
    var main = document.createElement('div');
    main.classList.add('light_label');

    if (light.status) main.style.color = 'green';
    else main.style.color = 'red';

    var color = document.createElement('div');
    color.classList.add('light_color');
    color.style.background = light.color;
    main.appendChild(color);

    var input = document.createElement('input');
    input.type = 'checkbox';
    input.id = 'light' + light.id;
    input.classList.add('light_check');
    input.onchange = (function() {
      var l = light;
      return function(evt) {
        lightsSelectionChange(l, evt.target.checked);
      }
    })();
    main.appendChild(input);

    var label = document.createElement('label');
    var text = document.createTextNode(light.name);
    label.htmlFor = 'light' + light.id;
    label.appendChild(text);
    main.appendChild(label);

    return main;
  }

  function lightsSelectionChange(light, checked) {
    if (checked) {
      if (lightGroupContains(light) == -1) lightsGroup.push(light);
    } else {
      var pos = lightGroupContains(light);
      if (pos > -1) lightsGroup.splice(pos, 1);
    }
  }

  function lightGroupContains(light) {
    for (var i in lightsGroup) {
      var aux = lightsGroup[i];
      if (aux.serviceId == light.serviceId && aux.id == light.id) return i;
    }

    return -1;
  }

  function saveScene() {
    var lights = lightsGroup.map(function(light) {
      return light.id;
    });

    var colors = lightsGroup.map(function(light) {
      return light.color;
    });

    if (lights.length == 0 || colors.length == 0) {
      Utils.showInfoPopup('Save scene', 'No lights selected, you must select at least one', 'Ok', function() {
        Utils.removePopup();
      });
      return;
    }

    Utils.showInputTextPopup('Save scene', 'Type a name for the scene', 'Scene name', 'Save', 'Cancel', function(name) {
      Utils.removePopup();
      if (name) {
        var scene = {
          name: name,
          lights: lights,
          colors: colors
        };

        sendSaveScene(scene, function(status) {
          var msg = '';
          if (status == 200) msg = 'Scene saved successfully';
          else if (status == 409) msg = 'There is already a scene called "' + name + '"';
          else msg = 'Unknown error, try again later';

          Utils.showInfoPopup('Save scene', msg, 'Ok', function() {
            Utils.removePopup();
          });
        });
      }
    });
  }

  function setPickerStatus(status) {
    lightsGroup.forEach(function(light) {
      setLightStatus(light.id, status);
    });
  }

  function setPickerHue(value) {
    color.hue = value;
    updatePickerColor();
  }

  function changePickerHue(value) {
    color.hue += value;
    if (color.hue < 0) color.hue = 0;
    if (color.hue > 360) color.hue = 360;
    hueSlider.value = color.hue;
    updatePickerColor();
  }

  function setPickerSat(value) {
    color.sat = value;
    updatePickerColor();
  }

  function changePickerSat(value) {
    color.sat += value;
    if (color.sat < 0) color.sat = 0;
    if (color.sat > 100) color.sat = 100;
    satSlider.value = color.sat;
    updatePickerColor();
  }

  function setPickerBri(value) {
    color.bri = value;
    updatePickerColor();
  }

  function changePickerBri(value) {
    color.bri += value;
    if (color.bri < 0) color.bri = 0;
    if (color.bri > 100) color.bri = 100;
    briSlider.value = color.bri;
    updatePickerColor();
  }

  function updatePickerColor() {
    colorPickerDiv.style.backgroundColor = 'hsl(' + color.hue + ', ' + color.sat + '%, ' + color.bri / 2 + '%)';
    satSlider.style.backgroundColor = 'hsl(' + color.hue + ', 100%, 50%)';
    briSlider.style.backgroundColor = 'hsl(' + color.hue + ', 100%, 50%)';

    var rgb = colorPickerDiv.style.backgroundColor.match(/\d+/g);
    setPickerColor(rgb.toString());
  }

  var queue = null;
  var running = false;
  function setPickerColor(rgb) {
    if (running) queue = rgb;
    else {
      running = true;
      triggerSetPickerColor(rgb);
    }
  }

  function triggerSetPickerColor(rgb) {
    var colorCode = btoa(rgb);
    var ids = btoa(lightsGroup.map(function(light){return light.id}).toString());
    setLightColor(ids, colorCode, function() {
      if(queue == null) running = false;
      else {
        var next = queue;
        queue = null;
        triggerSetPickerColor(next);
      }
    });
  }

  function setContainerWidth() {
    //var itemWidth = scenesDiv.childNodes[1].offsetWidth + 20;
    var itemWidth = 124;
    var containerWidth = contentPanelDiv.offsetWidth - 100;
    scenesDiv.style.width = parseInt(containerWidth / itemWidth) * itemWidth + 'px';
  }

  function setLightStatus(lightId, status) {
    var url = 'system/' + serviceId + '/switch/' + lightId + '/' + (status ? 1 : 0);
    HttpClient.sendRequest(url, 'GET', null, function(status, statusText, response) {
      if (status != 200) console.log("Switch set status error: " + status + " - " + statusText + " - " + response);
    }, {disableAuth: true, loading: false});
  }

  function setLightsStatus(status) {
    var url = 'system/' + serviceId + '/switches/' + (status ? 1 : 0);
    HttpClient.sendRequest(url, 'GET', null, function(status, statusText, response) {
      if (status != 200) console.log("Switch set status error: " + status + " - " + statusText + " - " + response);
    }, {disableAuth: true, loading: false});
  }

  function setLightColor(lightIds, color, listener) {
    var url = 'system/' + serviceId + '/switches/' + lightIds + '/extra/' + color;
    HttpClient.sendRequest(url, 'GET', null, function(status, statusText, response) {
      if (status != 200) console.log("Switch set status error: " + status + " - " + statusText + " - " + response);
      if (listener != null) listener();
    }, {disableAuth: true, loading: false});
  }

  function setLightsColor(sceneId) {
    var url = 'system/' + serviceId + '/switches/extra/';

    var data = {
      "action": 0,
      "sceneId": sceneId
    };

    HttpClient.sendRequest(url, 'POST', data, function(status, statusText, response) {
      if (status != 200) console.log("Switch set status error: " + status + " - " + statusText + " - " + response);
    }, {disableAuth: true, loading: false});
  }

  function sendSaveScene(scene, callback) {
    var url = 'system/' + serviceId + '/switches/extra/';

    var data = {
      "action": 1,
      "scene": scene
    };

    HttpClient.sendRequest(url, 'POST', data, function(status, statusText, response) {
      if (status != 200) console.log("Switch set status error: " + status + " - " + statusText + " - " + response);
      callback(status);
    }, {disableAuth: true, loading: false});
  }

  function sendRemoveScene(scene, callback) {
    var url = 'system/' + serviceId + '/switches/extra/';

    var data = {
        "action": 2,
        "sceneId": scene
    };

    HttpClient.sendRequest(url, 'POST', data, function(status, statusText, response) {
        if (status != 200) console.log("Switch set status error: " + status + " - " + statusText + " - " + response);
        callback(status);
    }, {disableAuth: true, loading: false});
  }

  return {
    init: init
  };
}());
