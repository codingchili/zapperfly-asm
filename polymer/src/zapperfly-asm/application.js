const LOADING = 'loading';
const ERROR = 'ERROR';
const ACCEPTED = 'ACCEPTED';

class Application {

    constructor() {
        this.handlers = [];
        this.loggedin = false;
        this.token = {};
    }

    authenticated(token) {
        this.loggedin = true;
        this.token = token;
        this.publish('authenticated', {});
    }

    onAuthenticated(callback) {
        this.subscribe('authenticated', callback);
    }

    role(test) {
        if (this.loggedin) {
            return this.token.properties['role'] === test;
        } else {
            return false;
        }
    }

    logout() {
        this.loggedin = false;
        this.token = {};
        this.publish('logout', {});
    }

    onLogout(callback) {
        this.subscribe('logout', callback);
    }

    subscribe(event, callback) {
        if (this.handlers[event] == null)
            this.handlers[event] = [];

        this.handlers[event].push(callback);
    }

    publish(event, data) {
        if (this.handlers[event])
            for (let subscriber = 0; subscriber < this.handlers[event].length; subscriber++)
                this.handlers[event][subscriber](data);
    }
}

var application = new Application();