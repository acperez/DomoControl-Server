

function init() {
  Menu.init();
  Switches.init();
  Systems.init();
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

/*
function btn_test() {
  var button = document.getElementById('button')
  button.addEventListener('click', function(evt) {
    if (button.classList.contains('on')) button.classList.remove('on')
    else button.classList.add('on')
    });

  var button2 = document.getElementById('button2')
  button2.addEventListener('click', function(evt) {
      if (button2.classList.contains('on')) button2.classList.remove('on')
      else button2.classList.add('on')
      });
}*/