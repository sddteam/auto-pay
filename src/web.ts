import { WebPlugin } from '@capacitor/core';

import type { AutoPayPlugin } from './definitions';

export class AutoPayWeb extends WebPlugin implements AutoPayPlugin {
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
  navigateGCash(options: any): Promise<any> {
    return options;
  }
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
