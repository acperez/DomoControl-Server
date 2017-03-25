'use strict';

var SwitchesSettings = (function() {

  var listDiv, namesPanel, groupsPanel, listMappingsDiv, addSwitchGroupItem, addSwitchBtn;

  var activeElementCloser = null;

  function init() {
    listDiv = document.getElementById('switches_settings_items');
    namesPanel  = document.getElementById('switches_settings_names_panel');
    groupsPanel = document.getElementById('switches_settings_groups_panel');
    listMappingsDiv = document.getElementById('switches_settings_list');

    document.getElementsByName('switches_settings_toggle').forEach(function(toggleButton) {
      toggleButton.addEventListener('click', function() { togglePanel(toggleButton.value); })
    });

    addSwitchGroupItem = new SwitchGroup(listener);
    addSwitchGroupItem.style.display = 'none';
    listMappingsDiv.appendChild(addSwitchGroupItem);
    addSwitchBtn = document.getElementById('switches_show_add_btn');
    addSwitchBtn.addEventListener('click', function() { showAddPanel(); });

    showSwitches();
  }

  function togglePanel(value) {
    if (value == 'names') {
      namesPanel.style.display = 'block';
      groupsPanel.style.display = 'none';
    } else if (value == 'groups') {
      namesPanel.style.display = 'none';
      groupsPanel.style.display = 'block';
    }
  }

  var listener = {
    onExpand: function(elementCloser) {
      if(activeElementCloser) activeElementCloser();
      activeElementCloser = elementCloser;
    },
    onHide: function() {
      activeElementCloser = null;
    },
    onCreate: function(name, mappings, callback) {
      createGroup(name, mappings, callback);
    },
    onDelete: function(switchId, item) {
      deleteGroup(switchId, function() {
        listMappingsDiv.removeChild(item);
      })
    },
    onUpdate: function(switchId, mappings, callback) {
      updateGroup(switchId, mappings, callback);
    }
  };

  function showAddPanel() {
    if (addSwitchGroupItem.style.display == 'none') {
      addSwitchGroupItem.style.display = 'block';
      addSwitchBtn.childNodes[1].src = 'img/minus.svg';

      if(activeElementCloser) activeElementCloser();
      activeElementCloser = showAddPanel;
    } else {
      addSwitchGroupItem.style.display = 'none';
      addSwitchBtn.childNodes[1].src = 'img/plus.svg';
      activeElementCloser = null;
    }
  }

  function showSwitches() {
    var switches = DomoData.switches();

    var list = document.createElement('ul');
    list.classList.add('switches_settings_list');
    switches.forEach(function(domoSwitch) {
      if (domoSwitch.available) {
        var element = renderSwitch(domoSwitch);
        list.appendChild(element);

        if (domoSwitch.serviceId == 3) {
          var groupElement = new SwitchGroup(listener, domoSwitch, DomoData.groups[domoSwitch.id]);
          listMappingsDiv.appendChild(groupElement);
        }
      }
    });

    listDiv.appendChild(list);
  }

  function renderSwitch(domoSwitch) {
    var alias = domoSwitch.alias ? domoSwitch.alias : domoSwitch.name;

    var name = document.createElement('span');
    var text = document.createTextNode(alias);
    name.appendChild(text);

    var input = document.createElement('input');
    input.classList.add('settings_switch_input');
    input.type = 'text';
    input.placeholder = alias;
    input.maxLength = 256;

    var btn = document.createElement('a');
    btn.classList.add('btn', 'settings_switch_btn');
    var btnText = document.createTextNode('Rename');
    btn.appendChild(btnText);
    btn.addEventListener('click', function() {
      var newAlias = input.value;
      var serviceId = domoSwitch.serviceId;
      var id = domoSwitch.id;
      var name = domoSwitch.name;
      var alias = domoSwitch.alias;
      if (newAlias != "" && newAlias != name && newAlias != alias) {
        setAlias(serviceId, id, newAlias, function(status, statusText, response) {
          if (status != 200) console.log("Switch alias set status error: " + status + " - " + statusText + " - " + response);
        });
      }
    });

    var span = document.createElement('span');
    span.appendChild(input);
    span.appendChild(btn);

    var p = document.createElement('p');
    p.classList.add('settings_switch_item');
    p.appendChild(name);
    p.appendChild(span);

    var item = document.createElement('li');
    item.appendChild(p);

    return item;
  }

  function setAlias(serviceId, id, alias, callback) {
    var url = 'system/' + serviceId + '/switch/' + id + '/alias/' + alias;
    HttpClient.sendRequest(url, 'GET', null, callback, {disableAuth: true, loading: false})
  }

  function createGroup(name, mappings, callback) {
    var data = {
      name: name,
      switches: mappings
    };

    if (!data.name || data.name == '') {
      Utils.showInfoPopup('Add switch group', 'Empty group name, type a name for the new group', 'Ok', function() {
        Utils.removePopup();
      });
    } else if (data.switches.length < 1) {
      Utils.showInfoPopup('Add switch group', 'Missing switches in the group, select at least one to create a new group', 'Ok', function() {
        Utils.removePopup();
      });
    } else {
      var url = '/system/0/group/';

      HttpClient.sendRequest(url, 'POST', data, function(status, statusText, response) {
        if (status != 200) console.log("Switch alias set status error: " + status + " - " + statusText + " - " + response);
        else {
          showAddPanel();
          callback();
        }
      }, {disableAuth: true, loading: false});
    }
  }

  function deleteGroup(switchId, callback) {
    var url = '/system/0/group/' + switchId;
    HttpClient.sendRequest(url, 'DELETE', null, function(status, statusText, response) {
      if (status != 200) console.log("Switch alias set status error: " + status + " - " + statusText + " - " + response);
      else {
        callback();
      }
    }, {disableAuth: true, loading: false});
  }

  function updateGroup(switchId, mappings, callback) {
    var url = '/system/0/group/' + switchId;
    HttpClient.sendRequest(url, 'PUT', mappings, function(status, statusText, response) {
      if (status != 200) console.log("Switch alias set status error: " + status + " - " + statusText + " - " + response);
      else callback();
    }, {disableAuth: true, loading: false});
  }

  return {
    init: init
  };
}());
