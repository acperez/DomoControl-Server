'use strict';

var HttpClient = {

  _XMLHttpFactories: [
    function () {return new XMLHttpRequest()},
    function () {return new ActiveXObject("Msxml3.XMLHTTP")},
    function () {return new ActiveXObject("Msxml2.XMLHTTP.6.0")},
    function () {return new ActiveXObject("Msxml2.XMLHTTP.3.0")},
    function () {return new ActiveXObject("Msxml2.XMLHTTP")},
    function () {return new ActiveXObject("Microsoft.XMLHTTP")}
  ],

  _initXMLHTTPObject: function _initXMLHTTPObject() {
    for (var i in this._XMLHttpFactories) {
      try {
        this._XMLHttpFactories[i]();
        this._xmlHttpObject = this._XMLHttpFactories[i];
        return;
      } catch (e) {
        continue;
      }
    }

    this._xmlHttpObject = null;
  },

  popup: null,

  loadingMsg: 'Cargando',

  _xmlHttpObject: null,

  /*
   * options = {
   *   disableAuth: true | false - Prevents login popup on 401
   *   loading:     true | false - Show loading popup while request
   *   msg:         string       - Loading message to show if loading is true
   * }
   */
  sendRequest: function sendRequest(url, method, params, callback, options) {
    var req = this._xmlHttpObject();

    if (!req) {
      return;
    }

    var disableAuth = options && options.disableAuth;
    var loading = options && options.loading;
    var loadingMsg = options && options.msg ? options.msg : this.loadingMsg;

    if (loading) {
      this.loadingPopup(loadingMsg);
    }

    req.onreadystatechange = function () {
      if (req.readyState != 4) return;

      if (req.readyState == 4) {
        if (loading) {
          this.hiddePopup();
        }

        if (req.status == 401 && !disableAuth) {
          this.loginPopup(function() {
            HttpClient.sendRequest(url, method, params, callback, options);
          })
          return;
        }

        var response = null;
        try {
          response = JSON.parse(req.responseText);
        } catch (ex) {}
        callback(req.status, req.statusText, response);
      }
    }.bind(this);

    if (method == 'GET') {
      var params_string = "";

      if (params) {
        for (var name in params) {
          if (params[name]) {
            params_string += "&" + name + "=" + encodeURI(params[name]);
          }
        }
        params_string = params_string.substring(1);
        url += '?' + params_string;
      }

      req.open(method, url, true);
      req.send(params_string);
      return;
    }

    req.open(method, url, true);
    req.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
    var body = params ? JSON.stringify(params) : '';
    req.send(body);
  },

  sendFormData: function sendRequest(url, method, data, callback, options) {
    var req = this._xmlHttpObject();

    if (!req) {
      return;
    }

    var disableAuth = options && options.disableAuth;
    var loading = options && options.loading;
    var loadingMsg = options && options.msg ? options.msg : this.loadingMsg;

    if (loading) {
      this.loadingPopup(loadingMsg);
    }

    req.onreadystatechange = function () {
      if (req.readyState != 4) return;

      if (req.readyState == 4) {
        if (loading) {
          this.hiddePopup();
        }

        if (req.status == 401 && !disableAuth) {
          this.loginPopup(function() {
            HttpClient.sendFormData(url, method, data, callback, options);
          })
          return;
        }

        var response = null;
        try {
          response = JSON.parse(req.responseText);
        } catch (ex) {}
        callback(req.status, req.statusText, response);
      }
    }.bind(this);

    req.open(method, url, true);
    req.send(data);
  },

  getFile: function getFile(url, callback) {
    var req = this._xmlHttpObject();

    if (!req) {
      return;
    }

    req.open("GET", url);
    req.responseType = "blob";
    req.onload = function() {
      if (req.readyState != 4) return;

      if (req.status == 401 && this.onAuthError) {
        this.onAuthError(function() {
          HttpClient.getFile(url, callback);
        });
        return;
      }

      callback(req.status, req.response);
    }.bind(this);

    req.send();
  },

  renderPopup: function() {
    if (!this.popup) {
      var background = document.createElement('div');
      background.id = 'popup';
      background.classList.add('popup_background');

      var popupContainer = document.createElement('div');
      popupContainer.classList.add('popup_container');

      this.popup = document.createElement('div');
      popupContainer.appendChild(this.popup);

      background.appendChild(popupContainer);

      var body = document.body;
      body.appendChild(background);

      document.documentElement.style.overflow = 'hidden';
      return this.popup;
    }

    var container = this.popup.parentElement;
    container.removeChild(this.popup);
    this.popup = document.createElement('div');
    container.appendChild(this.popup);
    return this.popup;
  },

  hiddePopup: function() {
    if (this.popup) {
      document.body.removeChild(document.getElementById('popup'));
      document.documentElement.style.overflow = '';
      this.popup = null;
    }
  },

  loadingPopup: function(msg) {
    var popup = this.renderPopup();

    var title = document.createElement('p');
    title.classList.add('popup_title');
    title.appendChild(document.createTextNode(msg));

    var img = document.createElement('img');
    img.src = '/images/loading.gif';
    img.classList.add('popup_loading');

    popup.appendChild(title);
    popup.appendChild(img);
  },

  loginPopup: function(callback, loading) {
    var popup = this.renderPopup();

    var title = document.createElement('p');
    title.classList.add('popup_title');
    title.appendChild(document.createTextNode('Tu sesión ha caducado, introduce tus'));
    title.appendChild(document.createElement('br'));
    title.appendChild(document.createTextNode('credenciales para iniciar sesión otra vez'));

    var img = document.createElement('img');
    img.src = '/images/login.png';
    img.classList.add('popup_image');

    var error = document.createElement('p');
    error.classList.add('popup_error');
    var errorText = document.createTextNode('');
    error.appendChild(errorText);

    var user = document.createElement('input');
    user.type = 'text';
    user.classList.add('popup_input');
    user.placeholder = 'Email';
    user.maxLength = 256;
    var self = this;
    user.onfocus = (function() {
      return function() {
        self.clearErrors(user, errorText);
      }
    })();

    var pass = document.createElement('input');
    pass.type = 'password';
    pass.classList.add('popup_input');
    pass.placeholder = 'Contraseña';
    pass.maxLength = 40;
    pass.onfocus = (function() {
      return function() {
        self.clearErrors(pass, errorText);
      }
    })();

    var send = document.createElement('button');
    send.type = 'button';
    send.classList.add('button', 'popup_send');
    send.appendChild(document.createTextNode('Enviar'));
    send.onclick = (function() {
      var func = callback;
      return function() {
        self.clearErrors(user, errorText);
        self.clearErrors(pass, errorText);
        InputHelper.clearValidator();

        var email = InputHelper.validate(user, {required: true, length: [6, 256], op: 'email'}, self.onPopupLoginError.bind(error, errorText));
        if (InputHelper.validationErrors()) {
          return;
        }

        var password = InputHelper.validate(pass, {required: true, length: [1, 40], op: 'spaces'}, self.onPopupLoginError.bind(error, errorText));
        if (InputHelper.validationErrors()) {
          return;
        }

        var params = {
          email:    email,
          password: password
        }

        self.loginRequest(errorText, params, callback, loading);
      }
    })();

    popup.appendChild(title);
    popup.appendChild(img);
    popup.appendChild(user);
    popup.appendChild(pass);
    popup.appendChild(error);
    popup.appendChild(send);
  },

  clearErrors: function(element, error) {
    element.classList.remove('popup_input_error');
    error.nodeValue = '';
  },

  onPopupLoginError: function onPopupLoginError(errorTextObj, element, error) {
    this.classList.remove('text_fade_in');
    this.offsetWidth;
    this.classList.add('text_fade_in');
    element.classList.add('popup_input_error');
    errorTextObj.nodeValue = error;
  },

  loginRequest: function(errorObj, params, callback) {
    this.sendRequest('/api/session', 'POST', params, function(status, statusText, response) {
      HttpClient.processLoginResponse(status, statusText, response, errorObj, callback);
    }, true);
  },

  processLoginResponse: function(status, statusText, response, errorObj, callback) {
    if (status == 200) {
      this.hiddePopup();
      callback();
    } else if (status == 401) {
      errorObj.nodeValue = 'Usuario o contraseña incorrectos';
    } else {
      errorObj.nodeValue = 'Lo sentimos, se ha producido un error, intenetalo mas tarde';
    }
  }
};

HttpClient._initXMLHTTPObject();