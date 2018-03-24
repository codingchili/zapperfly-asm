const LOADING = 'loading';

const application = {
    handlers: [],
    token: null,
    authenticated: false,

    onAuthenticated: function (token) {
        this.authenticated = true;
        this.token = token;
        this.publish('authenticated', token);
    },

    onLogout: function () {
        this.authenticated = false;
        this.token = null;
        this.publish('logout');
    },

    subscribe: function (event, callback) {
        if (this.handlers[event] == null)
            this.handlers[event] = [];

        this.handlers[event].push(callback);
    },

    publish: function (event, data) {
        if (this.handlers[event])
            for (let subscriber = 0; subscriber < this.handlers[event].length; subscriber++)
                this.handlers[event][subscriber](data);
    }
};