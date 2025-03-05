'use strict';

var core = require('@capacitor/core');

const AutoPay = core.registerPlugin('AutoPay', {
    web: () => Promise.resolve().then(function () { return web; }).then((m) => new m.AutoPayWeb()),
});

class AutoPayWeb extends core.WebPlugin {
    checkOverlayPermission() {
        const result = {
            value: true
        };
        return new Promise((resolve, reject) => {
            try {
                resolve(result);
            }
            catch (error) {
                reject(false);
            }
        });
    }
    enableOverlayPermission() {
        const result = {
            value: true
        };
        return new Promise((resolve, reject) => {
            try {
                resolve(result);
            }
            catch (error) {
                reject(false);
            }
        });
    }
    checkAccessibility() {
        const result = {
            value: true
        };
        return new Promise((resolve, reject) => {
            try {
                resolve(result);
            }
            catch (error) {
                reject(false);
            }
        });
    }
    enableAccessibility() {
        const result = {
            value: true
        };
        return new Promise((resolve, reject) => {
            try {
                resolve(result);
            }
            catch (error) {
                reject(false);
            }
        });
    }
    stopNavigation(options) {
        return options;
    }
    performGesture(options) {
        return new Promise((resolve, reject) => {
            try {
                resolve(options);
            }
            catch (error) {
                reject(error);
            }
        });
    }
    startAutoPay(options) {
        return options;
    }
    navigateGCash(options) {
        return options;
    }
    async echo(options) {
        console.log('ECHO', options);
        return options;
    }
}

var web = /*#__PURE__*/Object.freeze({
    __proto__: null,
    AutoPayWeb: AutoPayWeb
});

exports.AutoPay = AutoPay;
//# sourceMappingURL=plugin.cjs.js.map
