'use strict';

function SwitchGroup(listener, domoSwitch, groupProperties) {

  var mappings = groupProperties ? groupProperties.switches : [];

  var expanded = false;
  var title = null;
  var titleInput = null;
  var toolbar = null;
  var addBtn = createAddBtn();
  var header = createHeader();
  var list = createMappingList();

  if(!domoSwitch) {
    titleInput = createTitleInput();
    title = createAddTitle();
    toolbar = createAddToolbar();
  } else {
    title = createTitle();
    toolbar = createToolbar();
    header.style.display = 'none';
    list.style.display = 'none';
    toolbar.style.display = 'none';
  }

  var component = createComponent();

  var select = updateSelect();

  function updateSelect() {
    var switches = DomoData.switches();

    var selectObj = document.createElement('select');
    selectObj.classList.add('settings_switch_mapping_select');

    switches.forEach(function (item) {
      if (item.serviceId != 3) {
        var index = searchItem({
          serviceId: item.serviceId,
          switchId: item.id
        });

        if (index < 0) {
          var selectOption = document.createElement('option');
          selectOption.dataset.service = item.serviceId;
          selectOption.dataset.switch = item.id;

          var selectText = document.createTextNode(item.alias ? item.alias : item.name);
          selectOption.appendChild(selectText);
          selectObj.appendChild(selectOption);
        }
      }
    });

    if (selectObj.options.length > 0) {
      list.appendChild(selectObj);
      addBtn.style.display = 'block';
    } else {
      selectObj = null;
      addBtn.style.display = 'none';
    }

    return selectObj;
  }

  function addMapping() {
    if (select) {
      var option = select.options[select.selectedIndex];
      var value = {
        serviceId: parseInt(option.dataset.service),
        switchId: option.dataset.switch
      };
      mappings.push(value);

      var selectParent = select.parentNode;
      selectParent.removeChild(select);
      var mapping = document.createElement('p');
      mapping.classList.add('settings_mapping_item');

      var btnDel = document.createElement('a');
      btnDel.classList.add('button_pick', 'button_pick_left', 'settings_add_btn_small');
      var delImg = document.createElement('img');
      delImg.classList.add('settings_btn_img_small');
      delImg.src = 'img/close_small.svg';
      btnDel.appendChild(delImg);
      mapping.appendChild(btnDel);

      var span = document.createElement('span');
      span.classList.add('settings_mapping_item_span');
      var text = document.createTextNode(option.text);
      span.appendChild(text);
      mapping.appendChild(span);

      selectParent.appendChild(mapping);

      btnDel.addEventListener('click', function() {
        var index = searchItem(value);
        if (index > -1) mappings.splice(mappings.indexOf(value), 1);
        list.removeChild(mapping);
        if (select) list.removeChild(select);
        select = updateSelect();
      });
    }

    select = updateSelect();
  }

  function createComponent() {
    var element = document.createElement('div');
    element.classList.add('switches_settings_group_item');
    element.appendChild(title);
    element.appendChild(header);
    element.appendChild(list);
    element.appendChild(toolbar);
    return element;
  }

  function hideControl() {
    header.style.display = 'none';
    list.style.display = 'none';
    toolbar.style.display = 'none';
    expanded = false;
  }

  function createTitleInput() {
    var input = document.createElement('input');
    input.classList.add('settings_switch_input');
    input.type = 'text';
    input.placeholder = 'Switch name';
    input.maxLength = 256;

    return input;
  }

  function createAddTitle() {
    var titleDiv = document.createElement('div');
    titleDiv.classList.add('settings_switch_item');
    var nameP = document.createElement('p');
    var nameText = document.createTextNode('Name');
    nameP.appendChild(nameText);
    titleDiv.appendChild(nameP);
    titleDiv.appendChild(titleInput);

    return titleDiv;
  }

  function createTitle() {
    var titleDiv = document.createElement('div');
    titleDiv.classList.add('settings_switch_item');
    var nameP = document.createElement('p');
    var nameText = document.createTextNode(domoSwitch.alias ? domoSwitch.alias : domoSwitch.name);
    nameP.appendChild(nameText);
    titleDiv.appendChild(nameP);

    var infoBtn = document.createElement('a');
    infoBtn.classList.add('button_pick', 'button_pick_left', 'settings_add_btn');
    var infoImg = document.createElement('img');
    infoImg.classList.add('settings_btn_img');
    infoImg.src = 'img/info.svg';
    infoBtn.addEventListener('click', function() {
      if (expanded) {
        header.style.display = 'none';
        list.style.display = 'none';
        toolbar.style.display = 'none';
        expanded = false;
        listener.onHide();
      } else {
        header.style.display = 'flex';
        list.style.display = 'block';
        toolbar.style.display = 'block';
        expanded = true;
        listener.onExpand(hideControl);
      }
    });

    infoBtn.appendChild(infoImg);
    titleDiv.appendChild(infoBtn);

    return titleDiv;
  }

  function createAddBtn() {
    var addBtn = document.createElement('a');
    addBtn.classList.add('button_pick', 'button_pick_left', 'settings_add_btn');
    var addImg = document.createElement('img');
    addImg.classList.add('settings_btn_img');
    addImg.src = 'img/plus.svg';
    addBtn.appendChild(addImg);

    addBtn.addEventListener('click', function() {
      addMapping();
    });

    return addBtn;
  }

  function createHeader() {
    var switchesHeader = document.createElement('div');
    switchesHeader.classList.add('settings_switch_item');
    var label = document.createElement('p');
    var labelText = document.createTextNode('Switches');
    label.appendChild(labelText);
    switchesHeader.appendChild(label);
    switchesHeader.appendChild(addBtn);

    return switchesHeader;
  }

  function createMappingList() {
    var mappingsDiv = document.createElement('div');
    mappingsDiv.classList.add('settings_switch_mapping_list');

    mappings.forEach(function (groupSwicth) {
      var mapping = document.createElement('p');
      mapping.classList.add('settings_mapping_item');

      var btnDel = document.createElement('a');
      btnDel.classList.add('button_pick', 'button_pick_left', 'settings_add_btn_small');
      var delImg = document.createElement('img');
      delImg.classList.add('settings_btn_img_small');
      delImg.src = 'img/close_small.svg';
      btnDel.addEventListener('click', function() {
        var index = searchItem(groupSwicth);
        if (index > -1) mappings.splice(mappings.indexOf(groupSwicth), 1);
        list.removeChild(mapping);
        if (select) list.removeChild(select);
        select = updateSelect();
      });
      btnDel.appendChild(delImg);
      mapping.appendChild(btnDel);

      var span = document.createElement('span');
      span.classList.add('settings_mapping_item_span');

      var domoSwitch = DomoData.systems[groupSwicth.serviceId].switches.find(function (element) {
        return element.id == groupSwicth.switchId;
      });

      var text = document.createTextNode(domoSwitch.alias ? domoSwitch.alias : domoSwitch.name);
      span.appendChild(text);
      mapping.appendChild(span);

      mappingsDiv.appendChild(mapping);
    });

    return mappingsDiv;
  }

  function createAddToolbar() {
    var toolBar = document.createElement('div');
    toolBar.classList.add('settings_switch_btn_container');
    var saveBtn = document.createElement('a');
    saveBtn.classList.add('btn', 'settings_switch_btn_big');
    var saveText = document.createTextNode('Save');
    saveBtn.appendChild(saveText);
    toolBar.appendChild(saveBtn);

    saveBtn.addEventListener('click', function() {
      listener.onCreate(titleInput.value, mappings, function() {
        mappings = [];
        titleInput.value = '';
        while (list.firstChild) {
          list.removeChild(list.firstChild);
        }
        select = updateSelect();
      });
    });

    return toolBar;
  }

  function createToolbar() {
    var toolBar = document.createElement('div');
    toolBar.classList.add('settings_switch_btn_container');
    var delBtn = document.createElement('a');
    delBtn.classList.add('btn', 'settings_switch_btn_big');
    var delText = document.createTextNode('Delete');
    delBtn.appendChild(delText);
    var updateBtn = document.createElement('a');
    updateBtn.classList.add('btn', 'settings_switch_btn_big');
    var updateText = document.createTextNode('Update');
    updateBtn.appendChild(updateText);
    toolBar.appendChild(delBtn);
    toolBar.appendChild(updateBtn);

    delBtn.addEventListener('click', function() {
      listener.onDelete(domoSwitch.id, component);
    });

    updateBtn.addEventListener('click', function() {
      listener.onUpdate(domoSwitch.id, mappings, function() {
        header.style.display = 'none';
        list.style.display = 'none';
        toolbar.style.display = 'none';
        expanded = false;
        listener.onHide();
      });
    });

    return toolBar;
  }

  function searchItem(value) {
    return mappings.findIndex(function (element) {
      return element.serviceId === value.serviceId && element.switchId === value.switchId
    });
  }

  return component;
}
