var capacitorAutoPay = (function (exports, core) {
    'use strict';

    const AutoPay = core.registerPlugin('AutoPay', {
        web: () => Promise.resolve().then(function () { return web; }).then((m) => new m.AutoPayWeb()),
    });

    class AutoPayWeb extends core.WebPlugin {
        checkOverlayPermission() {
            const result = {
                value: false
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

    return exports;

})({}, capacitorExports);
//# sourceMappingURL=plugin.js.map
