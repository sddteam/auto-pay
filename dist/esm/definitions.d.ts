import { PluginListenerHandle } from "@capacitor/core";
export interface PayMateOptions {
    app: string;
    base64: string;
}
export declare enum SupportedApps {
    GCASH = "GCASH",
    MAYA = "MAYA",
    METROBANK = "METROBANK",
    BDO = "BDO"
}
export interface AutoPayPlugin {
    checkAccessibility(): Promise<any>;
    enableAccessibility(): Promise<any>;
    checkOverlayPermission(): Promise<any>;
    enableOverlayPermission(): Promise<any>;
    addListener(eventName: 'warning', listenerFunc: (data: {
        message: string;
    }) => void): Promise<PluginListenerHandle>;
    addListener(eventName: 'error', listenerFunc: (data: {
        message: string;
    }) => void): Promise<PluginListenerHandle>;
    removeAllListeners(): Promise<void>;
    startAutoPay(options: PayMateOptions): Promise<any>;
}
