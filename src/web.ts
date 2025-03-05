import { WebPlugin } from '@capacitor/core';

import type { AutoPayPlugin } from './definitions';

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
  stopNavigation(options: any): Promise<any> {
    return options;
  }
  performGesture(options: { x: number; y: number; }): Promise<any> {
    return new Promise((resolve, reject) => {
      try{
        resolve(options);
      }catch(error){
        reject(error);
      }
    });
  }
  startAutoPay(options: any): Promise<any>{
    return options;
  }
  navigateGCash(options: any): Promise<any> {
    return options;
  }
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
