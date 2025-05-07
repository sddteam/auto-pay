import { WebPlugin } from '@capacitor/core';

import type { AutoPayPlugin, PayMateOptions } from './definitions';

export class AutoPayWeb extends WebPlugin implements AutoPayPlugin {
  checkOverlayPermission(): Promise<any> {
    const result = {
      value: true
    }
    return new Promise((resolve, reject) => {
      try{
        resolve(result);
      }catch(error){
        reject(false);
      }
    });
  }
  enableOverlayPermission(): Promise<any> {
    const result = {
      value: true
    }
    return new Promise((resolve, reject) => {
      try{
        resolve(result);
      }catch(error){
        reject(false);
      }
    });
  }
  checkAccessibility(): Promise<any> {
    const result = {
      value: true
    }
    return new Promise((resolve, reject) => {
      try{
        resolve(result);
      }catch(error){
        reject(false);
      }
    });
  }
  enableAccessibility(): Promise<any> {
    const result = {
      value: true
    }
    return new Promise((resolve, reject) => {
      try{
        resolve(result);
      }catch(error){
        reject(false);
      }
    });
  }
  startAutoPay(options: PayMateOptions): Promise<any>{
    return Promise.resolve(options);
  }
}
