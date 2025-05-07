'use strict';

var core = require('@capacitor/core');

exports.SupportedApps = void 0;
(function (SupportedApps) {
    SupportedApps["GCASH"] = "GCASH";
    SupportedApps["MAYA"] = "MAYA";
    SupportedApps["METROBANK"] = "METROBANK";
    SupportedApps["BDO"] = "BDO";
})(exports.SupportedApps || (exports.SupportedApps = {}));

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
    startAutoPay(options) {
        return Promise.resolve(options);
    }
}

var web = /*#__PURE__*/Object.freeze({
    __proto__: null,
    AutoPayWeb: AutoPayWeb
});

exports.AutoPay = AutoPay;
//# sourceMappingURL=plugin.cjs.js.map
