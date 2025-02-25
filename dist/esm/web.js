import { WebPlugin } from '@capacitor/core';
export class AutoPayWeb extends WebPlugin {
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
//# sourceMappingURL=web.js.map