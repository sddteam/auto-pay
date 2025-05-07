import { WebPlugin } from '@capacitor/core';
export class AutoPayWeb extends WebPlugin {
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
//# sourceMappingURL=web.js.map