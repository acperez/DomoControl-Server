'use strict';

var Wemo = (function() {

  var usagePanel, usageSelect, usageDiv, historyPanel, monitorDataDiv, nextMonthBtn;

  var historyVisited = false;
  var historyMonth = 0;

  var serviceId = 2;

  function init() {
    usagePanel = document.getElementById('wemo_usage_panel');
    usageSelect = document.getElementById('usage_device_selector');
    usageDiv = document.getElementById('usage_panel');
    historyPanel = document.getElementById('wemo_history_panel');
    document.getElementById('wemo_btn_refresh').addEventListener('click', function() { refreshMonitors(); });

    monitorDataDiv = document.getElementById('wemo_data');
    showDevicesData();

    document.getElementsByName('wemo_toggle').forEach(function(toggleButton) {
        toggleButton.addEventListener('click', function() { togglePanel(toggleButton.value); })
    });

    document.getElementById('wemo_btn_refresh_history').addEventListener('click', function() { loadHistory(historyMonth); });
    document.getElementById('wemo_btn_clear_history').addEventListener('click', function() { clearHistory(); });
    document.getElementById('wemo_prev_month').addEventListener('click', function() { loadHistoryMonth(false); });
    nextMonthBtn = document.getElementById('wemo_next_month');
    nextMonthBtn.addEventListener('click', function() { loadHistoryMonth(true); });

    WemoHistory.init();
  }

  function togglePanel(value) {
    if (value == 'usage') {
      usagePanel.style.display = 'block';
      historyPanel.style.display = 'none';
    } else if (value == 'history') {
      if (!historyVisited) {
        loadHistory(historyMonth);
        historyVisited = true;
      }
      usagePanel.style.display = 'none';
      historyPanel.style.display = 'block';
    }
  }

  function showDevicesData() {
    var monitorData = DomoData.monitors;

    monitorData.forEach(function(data) {
      var selectOption = document.createElement('option');
      var selectText = document.createTextNode(data.id);
      selectOption.appendChild(selectText);
      usageSelect.appendChild(selectOption);

      var dataDiv = renderDataForDevice(data);
      usageDiv.appendChild(dataDiv);
    });

    document.getElementById('usage-' + usageSelect.value).style.display = 'block';
    usageSelect.setAttribute("data-prev", usageSelect.value);

    usageSelect.onchange = function() {
      document.getElementById('usage-' + usageSelect.getAttribute('data-prev')).style.display = 'none';
      document.getElementById('usage-' + usageSelect.value).style.display = 'block';
      usageSelect.setAttribute("data-prev", usageSelect.value);
    };
  }

  function renderDataForDevice(data) {
    var contentDiv = document.createElement('div');
    contentDiv.id = 'usage-' + data.id;
    contentDiv.style.display = 'none';

    var timestamp = new Date(data.lastStateChange);
    var lastStateChange = renderPair('Last state change', timestamp.toString());
    lastStateChange.classList.add('wemo_data_pair_first');

    var lastOnFor = renderPair('Last \'on\' state duration', getDuration(data.lastOnFor));
    var onToday = renderPair('Time \'on\' today', getDuration(data.onToday));
    var onTotal = renderPair('Time \'on\' total', getDuration(data.onTotal));
    var timeSpan = renderPair('Time span', getDuration(data.timeSpan));
    var averagePower = renderPair('Average power', data.averagePowerW + 'W');
    var current = renderPair('Current', data.currentW + 'W');
    var energyToday = renderPair('Energy used today', data.energyTodayWh + 'Wh');
    var energyTotal = renderPair('Total energy used', data.energyTotalWh + 'Wh');
    var standbyLimit = renderPair('Standby limit', data.standbyLimitW + 'W');

    contentDiv.appendChild(lastStateChange);
    contentDiv.appendChild(lastOnFor);
    contentDiv.appendChild(onToday);
    contentDiv.appendChild(onTotal);
    contentDiv.appendChild(timeSpan);
    contentDiv.appendChild(averagePower);
    contentDiv.appendChild(current);
    contentDiv.appendChild(energyToday);
    contentDiv.appendChild(energyTotal);
    contentDiv.appendChild(standbyLimit);

    return contentDiv;
  }

  function updateUsage(data) {
    DomoData.monitors = data;

    while (usageDiv.firstChild) {
      usageDiv.removeChild(usageDiv.firstChild);
    }

    while (usageSelect.firstChild) {
      usageSelect.removeChild(usageSelect.firstChild);
    }

    showDevicesData();
  }

  function getDuration(duration) {
    var h = Math.round(duration / 3600);
    var m = Math.round((duration - (h * 3600)) / 60);
    var s = (duration - (h * 3600) - (m * 60));

    if (h < 10) h = "0" + h;
    if (m < 10) m = "0" + m;
    if (s < 10) s = "0" + s;

    return h + 'h ' + m + 'm ' + s + 's';
  }

  function renderPair(label, value) {
    var pairDiv = document.createElement('div');
    pairDiv.classList.add('wemo_data_pair');

    var labelDiv = document.createElement('div');
    labelDiv.classList.add('wemo_data_label');
    var labelText = document.createTextNode(label);
    labelDiv.appendChild(labelText);

    var valueDiv = document.createElement('div');
    valueDiv.classList.add('wemo_data_value');
    var valueText = document.createTextNode(value);
    valueDiv.appendChild(valueText);

    var clear = document.createElement('div');
    clear.classList.add('clear');

    pairDiv.appendChild(labelDiv);
    pairDiv.appendChild(valueDiv);
    pairDiv.appendChild(clear);

    return pairDiv;
  }

  function refreshMonitors() {
    refresh(function(status, statusText, response) {
      if (status != 200) console.log("Switch set status error: " + status + " - " + statusText + " - " + response);
      else {
        updateUsage(response);
      }
    });
  }

  function refresh(callback) {
    var url = 'system/' + serviceId + '/plugs/usage';
    HttpClient.sendRequest(url, 'GET', null, callback, {disableAuth: true, loading: false});
  }

  function loadHistoryMonth(nextMonth) {
    if (nextMonth && historyMonth > 0) {
      historyMonth--;
      if (historyMonth == 0) {
        nextMonthBtn.classList.add('disabled');
      }
      loadHistory();
    }

    if (!nextMonth) {
      historyMonth++;
      nextMonthBtn.classList.remove('disabled');
      loadHistory();
    }
  }

  function loadHistory() {
    var monitorId = usageSelect.value;
    loadHistoryRequest(monitorId, function(status, statusText, response) {
      if (status != 200) console.log("Switch set status error: " + status + " - " + statusText + " - " + response);
      else {
        WemoHistory.load(response);
      }
    });
  }

  function loadHistoryRequest(id, callback) {
    var url = 'system/' + serviceId + '/plugs/usage/history/' + id + '/' + historyMonth;
    HttpClient.sendRequest(url, 'GET', null, callback, {disableAuth: true, loading: false});
  }

  function clearHistory() {
    var monitorId = usageSelect.value;

    var url = 'system/' + serviceId + '/plugs/usage/history/' + monitorId;
    HttpClient.sendRequest(url, 'DELETE', null, function(status, statusText, response) {
      if (status != 200) console.log("Switch set status error: " + status + " - " + statusText + " - " + response);
      else {
        loadHistory();
      }
    }, {disableAuth: true, loading: false});
  }

  return {
    init: init
  };
}());
